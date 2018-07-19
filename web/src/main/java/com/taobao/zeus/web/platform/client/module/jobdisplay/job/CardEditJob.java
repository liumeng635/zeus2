package com.taobao.zeus.web.platform.client.module.jobdisplay.job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.text.shared.AbstractSafeHtmlRenderer;
import com.sencha.gxt.cell.core.client.form.ComboBoxCell.TriggerAction;
import com.sencha.gxt.core.client.Style.Side;
import com.sencha.gxt.core.client.XTemplates;
import com.sencha.gxt.core.client.dom.ScrollSupport.ScrollMode;
import com.sencha.gxt.data.shared.LabelProvider;
import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.data.shared.ModelKeyProvider;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.container.FlowLayoutContainer;
import com.sencha.gxt.widget.core.client.container.HorizontalLayoutContainer;
import com.sencha.gxt.widget.core.client.container.HorizontalLayoutContainer.HorizontalLayoutData;
import com.sencha.gxt.widget.core.client.container.MarginData;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer.VerticalLayoutData;
import com.sencha.gxt.widget.core.client.event.BeforeSelectEvent;
import com.sencha.gxt.widget.core.client.event.BeforeSelectEvent.BeforeSelectHandler;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.event.SelectEvent.SelectHandler;
import com.sencha.gxt.widget.core.client.form.ComboBox;
import com.sencha.gxt.widget.core.client.form.FieldLabel;
import com.sencha.gxt.widget.core.client.form.FieldSet;
import com.sencha.gxt.widget.core.client.form.NumberField;
import com.sencha.gxt.widget.core.client.form.NumberPropertyEditor.IntegerPropertyEditor;
import com.sencha.gxt.widget.core.client.form.TextArea;
import com.sencha.gxt.widget.core.client.form.TextField;
import com.sencha.gxt.widget.core.client.tips.ToolTipConfig;
import com.taobao.zeus.web.platform.client.lib.codemirror.CodeMirror;
import com.taobao.zeus.web.platform.client.lib.codemirror.CodeMirror.CodeMirrorConfig;
import com.taobao.zeus.web.platform.client.module.jobdisplay.CenterTemplate;
import com.taobao.zeus.web.platform.client.module.jobdisplay.FormatUtil;
import com.taobao.zeus.web.platform.client.module.jobdisplay.job.FileUploadWidget.UploadCallback;
import com.taobao.zeus.web.platform.client.module.jobdisplay.job.ProcesserType.HiveP;
import com.taobao.zeus.web.platform.client.module.jobdisplay.job.ProcesserType.ZooKeeperP;
import com.taobao.zeus.web.platform.client.module.jobdisplay.job.processer.ZKProcesserWindow;
import com.taobao.zeus.web.platform.client.module.jobmanager.CheckableJobTree;
import com.taobao.zeus.web.platform.client.module.jobmanager.GroupJobTreeModel;
import com.taobao.zeus.web.platform.client.module.jobmanager.JobModel;
import com.taobao.zeus.web.platform.client.module.jobmanager.event.TreeNodeChangeEvent;
import com.taobao.zeus.web.platform.client.util.Callback;
import com.taobao.zeus.web.platform.client.util.RPCS;
import com.taobao.zeus.web.platform.client.util.Refreshable;
import com.taobao.zeus.web.platform.client.util.async.AbstractAsyncCallback;

/**
 * Job编辑功能
 * 
 * @author zhoufang
 * 
 */
public class CardEditJob extends CenterTemplate implements
		Refreshable<JobModel> {
	private TextButton upload = new TextButton("上传资源文件", new SelectHandler() {
		@Override
		public void onSelect(final SelectEvent event) {
			new FileUploadWidget("job", presenter.getJobModel().getId(),
					new UploadCallback() {
						@Override
						public void call(final String name, final String uri) {
							Map<String, String> map = new HashMap<String, String>();
							map.put("name", name);
							map.put("uri", uri);
							List<Map<String, String>> temp = new ArrayList<Map<String, String>>();
							temp.add(map);
							resources.setValue(resources.getValue()
									+ "\n"
									+ FormatUtil
											.convertResourcesToEditString(temp));

						}
					}).show();
		}
	});

	// 更新任务时，保存更新
	private TextButton save = new TextButton("保存", new SelectHandler() {
		@Override
		public void onSelect(final SelectEvent event) {
			if (!resources.validate() || !configs.validate()
					|| !baseScheduleType.validate() || !baseCron.validate()
					|| !baseDepJobs.validate() || !baseDepCycle.validate()) {
				return;
			}
			final JobModel model = presenter.getJobModel().copy();
			model.setLocalProperties(FormatUtil.parseProperties(configs
					.getValue()));
			model.setLocalResources(FormatUtil.parseResources(resources
					.getValue()));
			model.setName(baseName.getValue());
			model.setDesc(baseDesc.getValue());
			model.setScript(script.getValue());
			model.setDefaultTZ(tzField.getValue());
			model.setOffRaw(offField.getValue());
			model.setJobScheduleType(baseScheduleType.getValue());
			model.setHost(hostField.getValue());
			if (baseScheduleType.getValue().equals(JobModel.DEPEND_JOB)
					|| baseScheduleType.getValue().equals(JobModel.CYCLE_JOB)) {
				String depJobStr = baseDepJobs.getValue();
				if (depJobStr != null && !"".equals(depJobStr.trim())) {
					String[] value = depJobStr.split(",");
					List<String> v = new ArrayList<String>();
					for (String s : value) {
						v.add(s);
					}
					model.setDependencies(v);
				} else {
					model.setDependencies(new ArrayList<String>());
				}
				if (baseDepCycle.getValue().get("value")
						.equalsIgnoreCase("sameday")) {
					model.getLocalProperties().put(CardInfo.DEPENDENCY_CYCLE,
							"sameday");
				} else {
					model.getLocalProperties()
							.remove(CardInfo.DEPENDENCY_CYCLE);
				}
			} else {
				model.setCronExpression(baseCron.getValue());
			}

			if (model.getJobRunType().equals(JobModel.MapReduce)) {
				baseMain.setAllowBlank(false);
				if (!baseMain.validate()) {
					return;
				}
				model.getLocalProperties().put(CardInfo.JAVA_MAIN_KEY,
						baseMain.getValue());
			}
			model.setJobCycle(jobCycle.getValue().get("value"));
			model.getPreProcessers().clear();

			model.getPostProcessers().clear();
			// Hive处理器配置
			/*if (notNullOrEmpty(outputTableField.getValue())
					|| notNullOrEmpty(syncTableField.getValue())) {
				HiveP p = new HiveP();
				if (notNullOrEmpty(outputTableField.getValue())) {
					p.setOutputTables(outputTableField.getValue());
					if (keepDaysField.getValue() != null) {
						p.setKeepDays(keepDaysField.getValue().toString());
					}
					if (driftPercentField.getValue() != null) {
						p.setDriftPercent(driftPercentField.getValue()
								.toString());
					}
				}
				if (notNullOrEmpty(syncTableField.getValue())) {
					p.setSyncTables(syncTableField.getValue());
				}
				model.getPreProcessers().add(p.getJsonObject());
				model.getPostProcessers().add(p.getJsonObject());
			}*/
			// 自定义ZK
			/*if (zkWindow.isOn()) {
				model.getPostProcessers().add(
						zkWindow.getProcesserType().getJsonObject());
			}*/

			RPCS.getJobService().updateJob(model,
					new AbstractAsyncCallback<JobModel>() {
						@Override
						public void onSuccess(final JobModel result) {
							RPCS.getJobService().getUpstreamJob(model.getId(),
									new AbstractAsyncCallback<JobModel>() {
										@Override
										public void onSuccess(
												final JobModel result) {
											presenter
													.getPlatformContext()
													.getPlatformBus()
													.fireEvent(
															new TreeNodeChangeEvent(
																	result));
											presenter.display(result);
										}
									});
						}
					});
		}

		private boolean notNullOrEmpty(String value) {
			return value != null && !value.trim().isEmpty();
		}
	});

	//private TextButton zk = new TextButton("自定义ZK[关闭]");

	private FieldSet baseFieldSet;
	//private FieldSet hiveProcesserFieldSet;
	private FieldSet configFieldSet;
	private FieldSet scriptFieldSet;
	private FieldSet resourceField;

	private TextField baseName;
	private TextArea baseDesc;
	private ComboBox<String> baseScheduleType;
	private TextField baseCron;
	private TextField baseDepJobs;
	private ComboBox<Map<String, String>> baseDepCycle;
	private TextField baseMain;

	private FieldLabel depCycleWapper;
	private FieldLabel depJobsWapper;
	private FieldLabel cronWapper;
	private FieldLabel mainWapper;

	// add by gufei.wzy 辅助功能
	/*private TextField outputTableField;
	private FieldLabel outputTableLabel;
	private TextField syncTableField;
	private FieldLabel syncTableLabel;
	private NumberField<Integer> keepDaysField;
	private FieldLabel keepDaysLabel;
	private NumberField<Integer> driftPercentField;
	private FieldLabel driftPercentLabel;*/

	private CodeMirror script;
	private TextArea configs;
	private TextArea resources;
	private JobPresenter presenter;

	// 添加时区
	private FieldLabel tzWapper;
	private TextField tzField;
	private FieldLabel offWapper;
	private TextField offField;
	private FieldLabel cycleWapper;
	private ComboBox<Map<String, String>> jobCycle;
	private TextField hostField;

	//private ZKProcesserWindow zkWindow = new ZKProcesserWindow(zk);

	public CardEditJob(final JobPresenter presenter) {
		this.presenter = presenter;

		FlowLayoutContainer centerContainer = new FlowLayoutContainer();
		centerContainer.add(getBaseFieldSet(), new MarginData(3));
		//centerContainer.add(getHiveProcesserFieldSet(), new MarginData(3));
		centerContainer.add(getConfigFieldSet(), new MarginData(3));
		centerContainer.add(getScriptFieldSet(), new MarginData(3));
		centerContainer.add(getResourceField(), new MarginData(3));
		centerContainer.setScrollMode(ScrollMode.AUTO);
		setCenter(centerContainer);

		addButton(new TextButton("返回", new SelectHandler() {
			@Override
			public void onSelect(final SelectEvent event) {
				presenter.display(presenter.getJobModel());
			}
		}));
		/*zk.addBeforeSelectHandler(new BeforeSelectHandler() {
			@Override
			public void onBeforeSelect(BeforeSelectEvent event) {
				event.setCancelled(true);
				zkWindow.show();
			}
		});
		addButton(zk);*/
		addButton(upload);
		addButton(save);
	}
/*
	private FieldSet getHiveProcesserFieldSet() {
		if (hiveProcesserFieldSet == null) {
			hiveProcesserFieldSet = new FieldSet();
			hiveProcesserFieldSet.setCollapsible(true);
			hiveProcesserFieldSet.setHeadingText("辅助功能配置");
			hiveProcesserFieldSet.setHeight(160);
			VerticalLayoutContainer container = new VerticalLayoutContainer();
			outputTableField = new TextField();
			outputTableField.setWidth(550);
			outputTableField.setAutoValidate(true);
			outputTableField
					.setEmptyText("多个表用','分隔，用于产出检查和历史分区清理。只支持第一个分区字段为pt且最新分区为昨天的表");
			ToolTipConfig outputTableToolTip = new ToolTipConfig();
			outputTableToolTip = new ToolTipConfig();
			outputTableToolTip
					.setBodyHtml("<span style=\"display:inline;word-wrap:break-word;\">多个表用英文逗号分隔。用于产出检查和历史分区清理。</span>");
			outputTableToolTip.setTitleHtml("产出的表");
			outputTableToolTip.setMouseOffset(new int[] { 0, 0 });
			outputTableToolTip.setAnchor(Side.BOTTOM);
			outputTableToolTip.setMaxWidth(280);
			outputTableToolTip.setHideDelay(3000);
			// outputTableField.setToolTipConfig(outputTableToolTip);

			syncTableField = new TextField();
			syncTableField.setWidth(550);
			syncTableField.setAutoValidate(true);
			syncTableField.setEmptyText("多个表用';'分隔。参数之间用','分隔。");
			syncTableField
					.setToolTip("可选参数依次为 1.分区下子分区数目(默认1)2.要同步的日期与当天的差值（默认-1，也就是昨天）;<br/>如r_auction_auctions,2;ds_fdi_atplog_base,70,-5");
			ToolTipConfig syncTableToolTip = new ToolTipConfig();
			syncTableToolTip
					.setBodyHtml("<span style=\"display:inline;word-wrap:break-word;\">多个表用英文分号';'分隔。可选参数依次为分区下子分区数目，要同步的日期，参数之间用英文逗号分隔。用于同步天网表。</span>");
			syncTableToolTip.setTitleHtml("要同步的天网表");
			syncTableToolTip.setMouseOffset(new int[] { 0, 0 });
			syncTableToolTip.setAnchor(Side.TOP);
			syncTableToolTip.setMaxWidth(280);
			syncTableToolTip.setHideDelay(3000);
			// syncTableField.setToolTipConfig(syncTableToolTip);

			keepDaysField = new NumberField<Integer>(
					new IntegerPropertyEditor());
			keepDaysField.setEmptyText("天数之前的分区会自动删除");
			keepDaysField.setAutoValidate(true);

			driftPercentField = new NumberField<Integer>(
					new IntegerPropertyEditor());
			driftPercentField.setEmptyText("百分比（如30）");
			driftPercentField.setToolTip("分区没有正常产出或者分区大小变化超过这个比例会发出报警");
			driftPercentField.setAutoValidate(true);

			outputTableLabel = new FieldLabel(outputTableField, "产出的表名");
			outputTableLabel.setHeight(30);
			syncTableLabel = new FieldLabel(syncTableField, "阻塞同步的天网表");
			syncTableLabel.setHeight(30);
			keepDaysLabel = new FieldLabel(keepDaysField, "分区保留天数");
			keepDaysLabel.setHeight(30);
			driftPercentLabel = new FieldLabel(driftPercentField, "产出数据浮动报警");
			driftPercentLabel.setHeight(30);

			container.add(syncTableLabel);
			container.add(outputTableLabel);
			container.add(driftPercentLabel);
			container.add(keepDaysLabel);

			hiveProcesserFieldSet.add(container);
		}
		return hiveProcesserFieldSet;
	}*/

	@Override
	public void refresh(final JobModel t) {
		baseName.setValue(t.getName());
		baseDesc.setValue(t.getDesc());
		tzField.setValue(t.getDefaultTZ());
		offField.setValue(t.getOffRaw());
		if (t.getJobRunType().equals(JobModel.MapReduce)) {
			baseMain.setValue(t.getLocalProperties()
					.get(CardInfo.JAVA_MAIN_KEY));
			mainWapper.show();
		} else {
			mainWapper.hide();
		}

		baseCron.setValue(t.getCronExpression());
		if (baseCron.getValue() == null
				|| "".equals(baseCron.getValue().trim())) {
			baseCron.setValue("0 0 3 * * ?");// 设置一个默认值
		}
		String depcode = "";
		for (String s : t.getDependencies()) {
			depcode += s + ",";
		}
		if (depcode.endsWith(",")) {
			depcode = depcode.substring(0, depcode.length() - 1);
		}
		baseDepJobs.setValue(depcode);
		hostField.setValue(t.getHost());

		String cycle = t.getAllProperties().get(CardInfo.DEPENDENCY_CYCLE);
		if (cycle == null) {
			baseDepCycle.setValue(baseDepCycle.getStore().get(0), true);
		} else {
			for (Map<String, String> data : baseDepCycle.getStore().getAll()) {
				if (data.get("value").equals(cycle)) {
					baseDepCycle.setValue(data, true);
					break;
				}
			}
		}

		if (t.getJobScheduleType() == null) {
			baseScheduleType.setValue(baseScheduleType.getStore().get(0), true);
		} else {
			for (String data : baseScheduleType.getStore().getAll()) {
				if (t.getJobScheduleType().equals(data)) {
					baseScheduleType.setValue(data, true);
				}
			}
		}

		if (t.getJobCycle() == null) {
			jobCycle.setValue(jobCycle.getStore().get(0), true);
		} else {
			for (Map<String, String> data : jobCycle.getStore().getAll()) {
				if (data.get("value").equals(t.getJobCycle())) {
					jobCycle.setValue(data, true);
					break;
				}
			}
		}

		// 初始化一下各种处理器配置
		//zkWindow.setOff();
		/*outputTableField.setValue(null);
		keepDaysField.setValue(null);
		driftPercentField.setValue(null);
		syncTableField.setValue(null);
		for (String post : t.getPostProcessers()) {
			if (post != null) {
				ProcesserType p = ProcesserType.parse(post);
				if (p != null) {
					if (p.getId().equalsIgnoreCase("hive")) {
						HiveP hiveP = (HiveP) p;
						outputTableField.setValue(hiveP.getOutputTables());
						if (hiveP.getKeepDays() != null) {
							keepDaysField.setValue(Integer.parseInt(hiveP
									.getKeepDays()));
						}
						if (hiveP.getDriftPercent() != null) {
							driftPercentField.setValue(Integer.parseInt(hiveP
									.getDriftPercent()));
						}
						syncTableField.setValue(hiveP.getSyncTables());
					} else if (p.getId().equalsIgnoreCase("zookeeper")) {
						if (!((ZooKeeperP) p).getUseDefault()) {
							zkWindow.setProcesser((ZooKeeperP) p);
						}
					}
				}
			}
		}*/

		script.setValue(t.getScript() == null ? "" : t.getScript());
		if (JobModel.MapReduce.equals(t.getJobRunType())) {
			scriptFieldSet.hide();
			//hiveProcesserFieldSet.hide();
		} else {
			scriptFieldSet.show();
			//hiveProcesserFieldSet.show();
		}
		configs.setValue(FormatUtil.convertPropertiesToEditString(t
				.getLocalProperties()));
		resources.setValue(FormatUtil.convertResourcesToEditString(t
				.getLocalResources()));

	}

	interface ComboBoxTemplates extends XTemplates {

		@XTemplate("<div>{name}</div>")
		SafeHtml display(String name);

	}

	public FieldSet getBaseFieldSet() {
		if (baseFieldSet == null) {
			baseFieldSet = new FieldSet();
			baseFieldSet.setHeadingText("基本信息");
			baseFieldSet.setHeight(150);

			HorizontalLayoutContainer layoutContainer = new HorizontalLayoutContainer();
			baseFieldSet.add(layoutContainer);

			VerticalLayoutContainer leftContainer = new VerticalLayoutContainer();
			VerticalLayoutContainer rightContainer = new VerticalLayoutContainer();
			layoutContainer
					.add(leftContainer, new HorizontalLayoutData(400, 1));
			layoutContainer.add(rightContainer);

			baseName = new TextField();
			baseName.setWidth(150);

			tzField = new TextField();
			tzField.setWidth(150);

			offField = new TextField();
			offField.setWidth(150);
			offField.setToolTip("单位分钟");

			baseDesc = new TextArea();
			baseDesc.setWidth(150);
			baseDesc.setHeight(36);
			hostField = new TextField();
			hostField.setWidth(150);
			ListStore<String> scheduleTypeStore = new ListStore<String>(
					new ModelKeyProvider<String>() {
						@Override
						public String getKey(final String item) {
							return item;
						}
					});
			scheduleTypeStore.add(JobModel.INDEPEN_JOB);
			scheduleTypeStore.add(JobModel.DEPEND_JOB);
			scheduleTypeStore.add(JobModel.CYCLE_JOB);

			baseScheduleType = new ComboBox<String>(scheduleTypeStore,
					new LabelProvider<String>() {
						@Override
						public String getLabel(final String item) {
							return item;
						}
					});
			baseScheduleType.setWidth(150);
			baseScheduleType.setTriggerAction(TriggerAction.ALL);
			baseScheduleType.setAllowBlank(false);
			baseScheduleType.setEditable(false);
			baseScheduleType
					.addValueChangeHandler(new ValueChangeHandler<String>() {
						@Override
						public void onValueChange(
								final ValueChangeEvent<String> event) {
							if (event.getValue().equals(JobModel.INDEPEN_JOB)) {
								cronWapper.show();
								baseCron.setAllowBlank(false);
								depCycleWapper.hide();
								depJobsWapper.hide();
								baseDepJobs.setAllowBlank(true);
								tzWapper.hide();
								offWapper.hide();
								cycleWapper.hide();
								hostField.show();
							}
							if (event.getValue().equals(JobModel.DEPEND_JOB)) {
								cronWapper.hide();
								tzWapper.hide();
								offWapper.hide();
								cycleWapper.hide();
								baseCron.setAllowBlank(true);
								depCycleWapper.show();
								depJobsWapper.show();
								baseDepJobs.setAllowBlank(false);
								hostField.show();
							}
							if (event.getValue().equals(JobModel.CYCLE_JOB)) {
								cronWapper.hide();
								depCycleWapper.hide();
								tzWapper.show();
								offWapper.show();
								cycleWapper.show();
								baseCron.setAllowBlank(true);
								depJobsWapper.show();
								baseDepJobs.setAllowBlank(true);
								hostField.show();
							}
						}
					});

			ListStore<Map<String, String>> jobCycleStore = new ListStore<Map<String, String>>(
					new ModelKeyProvider<Map<String, String>>() {
						@Override
						public String getKey(final Map<String, String> item) {
							return item.get("key");
						}
					});

			Map<String, String> day = new HashMap<String, String>();
			day.put("key", JobModel.JOB_CYCLE_DAY);
			day.put("value", "day");
			Map<String, String> hour = new HashMap<String, String>();
			hour.put("key", JobModel.JOB_CYCLE_HOUR);
			hour.put("value", "hour");

			jobCycleStore.add(day);
			jobCycleStore.add(hour);

			jobCycle = new ComboBox<Map<String, String>>(jobCycleStore,
					new LabelProvider<Map<String, String>>() {
						@Override
						public String getLabel(final Map<String, String> item) {
							return item.get("key");
						}
					}, new AbstractSafeHtmlRenderer<Map<String, String>>() {
						@Override
						public SafeHtml render(final Map<String, String> object) {
							ComboBoxTemplates t = GWT
									.create(ComboBoxTemplates.class);
							return t.display(object.get("key"));
						}

					});

			jobCycle.setWidth(150);
			jobCycle.setTriggerAction(TriggerAction.ALL);
			jobCycle.setAllowBlank(false);
			jobCycle.setEditable(false);

			baseCron = new TextField();
			baseCron.setWidth(150);
			baseDepJobs = new TextField();
			baseDepJobs.setWidth(150);
			baseDepJobs.setReadOnly(true);
			baseDepJobs.addHandler(new ClickHandler() {
				@Override
				public void onClick(final ClickEvent event) {
					final CheckableJobTree tree = new CheckableJobTree();
					tree.setSelectHandler(new SelectHandler() {
						@Override
						public void onSelect(final SelectEvent event) {
							List<GroupJobTreeModel> list = tree.getTree()
									.getCheckedSelection();// .getSelectionModel().getSelectedItems();
							String result = "";
							for (GroupJobTreeModel m : list) {
								if (m.isJob()) {
									result += m.getId() + ",";
								}
							}
							if (result.endsWith(",")) {
								result = result.substring(0,
										result.length() - 1);
							}
							baseDepJobs.setValue(result.toString(), true);
							baseDepJobs.validate();
						}
					});
					tree.show();
					tree.refresh(new Callback() {
						@Override
						public void callback() {
							tree.init(baseDepJobs.getValue());
						}
					});
				}
			}, ClickEvent.getType());
			ListStore<Map<String, String>> cycleStore = new ListStore<Map<String, String>>(
					new ModelKeyProvider<Map<String, String>>() {
						@Override
						public String getKey(final Map<String, String> item) {
							return item.get("key");
						}
					});
			Map<String, String> sameday = new HashMap<String, String>();
			sameday.put("key", "同一天");
			sameday.put("value", "sameday");
			Map<String, String> nolimit = new HashMap<String, String>();
			nolimit.put("key", "无限制");
			nolimit.put("value", " ");
			cycleStore.add(nolimit);
			cycleStore.add(sameday);
			baseDepCycle = new ComboBox<Map<String, String>>(cycleStore,
					new LabelProvider<Map<String, String>>() {
						@Override
						public String getLabel(final Map<String, String> item) {
							return item.get("key");
						}
					}, new AbstractSafeHtmlRenderer<Map<String, String>>() {
						@Override
						public SafeHtml render(final Map<String, String> object) {
							ComboBoxTemplates t = GWT
									.create(ComboBoxTemplates.class);
							return t.display(object.get("key"));
						}

					});
			baseDepCycle.setWidth(150);
			baseDepCycle.setTriggerAction(TriggerAction.ALL);
			baseDepCycle.setEditable(false);
			baseMain = new TextField();
			baseMain.setWidth(150);

			depCycleWapper = new FieldLabel(baseDepCycle, "依赖周期");
			depJobsWapper = new FieldLabel(baseDepJobs, "依赖任务");
			cronWapper = new FieldLabel(baseCron, "定时表达式");
			tzWapper = new FieldLabel(tzField, "时区");
			offWapper = new FieldLabel(offField, "启动延时");
			cycleWapper = new FieldLabel(jobCycle, "任务周期");
			mainWapper = new FieldLabel(baseMain, "Main类");
			leftContainer.add(new FieldLabel(baseName, "名称"),
					new VerticalLayoutData(1, -1));
			leftContainer.add(tzWapper, new VerticalLayoutData(1, -1));
			leftContainer.add(cycleWapper, new VerticalLayoutData(1, -1));
			leftContainer.add(new FieldLabel(baseDesc, "描述"),
					new VerticalLayoutData(1, -1));
			rightContainer.add(new FieldLabel(baseScheduleType, "调度类型"),
					new VerticalLayoutData(1, -1));
			rightContainer.add(cronWapper, new VerticalLayoutData(1, -1));
			rightContainer.add(depJobsWapper, new VerticalLayoutData(1, -1));
			rightContainer.add(depCycleWapper, new VerticalLayoutData(1, -1));
			rightContainer.add(offWapper, new VerticalLayoutData(1, -1));
			rightContainer.add(new FieldLabel(hostField, "Host"), new VerticalLayoutData(1, -1));
			leftContainer.add(mainWapper, new VerticalLayoutData(1, -1));

		}
		return baseFieldSet;
	}

	public FieldSet getConfigFieldSet() {
		if (configFieldSet == null) {
			configFieldSet = new FieldSet();
			configFieldSet.setCollapsible(true);
			configFieldSet.setHeadingText("配置项信息");
			configFieldSet.setWidth("96%");
			configFieldSet.setResize(false);
			configs = new TextArea();
			configs.setWidth(700);
			configs.setHeight(148);
			configFieldSet.add(configs);
		}
		return configFieldSet;
	}

	public FieldSet getResourceField() {
		if (resourceField == null) {
			resourceField = new FieldSet();
			resourceField.setCollapsible(true);
			resourceField.setHeadingText("资源信息");
			resourceField.setWidth("96%");
			resourceField.setResize(false);
			resources = new TextArea();
			resources.setWidth(700);
			resources.setHeight(148);
			resourceField.add(resources);
		}
		return resourceField;
	}

	public FieldSet getScriptFieldSet() {
		if (scriptFieldSet == null) {
			scriptFieldSet = new FieldSet();
			scriptFieldSet.setCollapsible(true);
			scriptFieldSet.setHeadingText("脚本");
			CodeMirrorConfig cmc = new CodeMirrorConfig();
			cmc.readOnly = false;
			script = new CodeMirror(cmc);
			scriptFieldSet.add(script);
			scriptFieldSet.setResize(false);
			scriptFieldSet.setWidth("96%");
		}
		return scriptFieldSet;
	}

}
