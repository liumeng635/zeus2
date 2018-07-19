package com.taobao.zeus.web.platform.client.module.jobdisplay.job;

import java.util.Map;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.sencha.gxt.core.client.dom.ScrollSupport.ScrollMode;
import com.sencha.gxt.widget.core.client.Dialog;
import com.sencha.gxt.widget.core.client.box.AlertMessageBox;
import com.sencha.gxt.widget.core.client.box.AutoProgressMessageBox;
import com.sencha.gxt.widget.core.client.box.ConfirmMessageBox;
import com.sencha.gxt.widget.core.client.box.ProgressMessageBox;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.container.FlowLayoutContainer;
import com.sencha.gxt.widget.core.client.container.HorizontalLayoutContainer;
import com.sencha.gxt.widget.core.client.container.HorizontalLayoutContainer.HorizontalLayoutData;
import com.sencha.gxt.widget.core.client.container.MarginData;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.BeforeHideEvent;
import com.sencha.gxt.widget.core.client.event.HideEvent;
import com.sencha.gxt.widget.core.client.event.HideEvent.HideHandler;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.event.SelectEvent.SelectHandler;
import com.sencha.gxt.widget.core.client.form.FieldLabel;
import com.sencha.gxt.widget.core.client.form.FieldSet;
import com.sencha.gxt.widget.core.client.info.Info;
import com.taobao.zeus.web.platform.client.app.PlacePath;
import com.taobao.zeus.web.platform.client.app.PlacePath.App;
import com.taobao.zeus.web.platform.client.app.PlacePath.DocType;
import com.taobao.zeus.web.platform.client.app.PlatformPlace;
import com.taobao.zeus.web.platform.client.lib.codemirror.CodeMirror;
import com.taobao.zeus.web.platform.client.lib.codemirror.CodeMirror.CodeMirrorConfig;
import com.taobao.zeus.web.platform.client.module.jobdisplay.AdminConfigWindow;
import com.taobao.zeus.web.platform.client.module.jobdisplay.CenterTemplate;
import com.taobao.zeus.web.platform.client.module.jobdisplay.job.ProcesserType.HiveP;
import com.taobao.zeus.web.platform.client.module.jobdisplay.job.ProcesserType.ZooKeeperP;
import com.taobao.zeus.web.platform.client.module.jobmanager.JobModel;
import com.taobao.zeus.web.platform.client.module.jobmanager.TreeKeyProviderTool;
import com.taobao.zeus.web.platform.client.module.jobmanager.event.TreeNodeChangeEvent;
import com.taobao.zeus.web.platform.client.util.RPCS;
import com.taobao.zeus.web.platform.client.util.Refreshable;
import com.taobao.zeus.web.platform.client.util.ToolUtil;
import com.taobao.zeus.web.platform.client.util.async.AbstractAsyncCallback;

public class CardInfo extends CenterTemplate implements Refreshable<JobModel>{

	private TextButton history=new TextButton("运行日志",new SelectHandler() {
		@Override
		public void onSelect(SelectEvent event) {
			presenter.displayHistory();
		}
	});
	
	private TextButton depGraph=new TextButton("依赖图",new SelectHandler() {
		@Override
		public void onSelect(SelectEvent event) {
			presenter.displayDepGraph();
		}
	});
	private TextButton editGroup=new TextButton("编辑",new SelectHandler() {
		@Override
		public void onSelect(SelectEvent event) {
			presenter.displayEditJob();
		}
	});
	private TextButton deleteGroup=new TextButton("删除",new SelectHandler() {
		@Override
		public void onSelect(SelectEvent event) {
			ConfirmMessageBox box=new ConfirmMessageBox("删除任务", "你确认删除此任务?");
			box.addHideHandler(new HideHandler() {
				@Override
				public void onHide(HideEvent event) {
					Dialog btn = (Dialog) event.getSource();
					if(btn.getHideButton().getText().equalsIgnoreCase("yes")){
						RPCS.getJobService().deleteJob(presenter.getJobModel().getId(), new AbstractAsyncCallback<Void>() {
							@Override
							public void onSuccess(Void result) {
								TreeNodeChangeEvent event=new TreeNodeChangeEvent();
								event.setNeedSelectProviderKey(TreeKeyProviderTool.genGroupProviderKey(presenter.getJobModel().getGroupId()));
								presenter.getPlatformContext().getPlatformBus().fireEvent(event);
							}
						});
					}
				}
			});
			box.show();
		}
	});
	private TextButton configAdmin=new TextButton("配置管理员",new SelectHandler() {
		@Override
		public void onSelect(SelectEvent event) {
			new AdminConfigWindow(presenter).show();
		}
	});
	private TextButton execute=new TextButton("手动执行",new SelectHandler() {
		@Override
		public void onSelect(SelectEvent event) {
			ConfirmMessageBox box=new ConfirmMessageBox("执行任务", "你确认手动执行该任务吗?");
			box.addHideHandler(new HideHandler() {
				@Override
				public void onHide(HideEvent event) {
					Dialog btn=(Dialog)event.getSource();
					if(btn.getHideButton().getText().equalsIgnoreCase("yes")){
						final AutoProgressMessageBox wait=new AutoProgressMessageBox("运行中","doing");
						wait.auto();
						wait.show();
						RPCS.getJobService().run(presenter.getJobModel().getId(), 1, new AbstractAsyncCallback<Void>() {
							@Override
							public void onSuccess(Void result) {
								wait.hide();
								Info.display("成功", "手动执行任务开始");
								
							}
							@Override
							public void onFailure(Throwable caught) {
								wait.hide();
								AlertMessageBox alert=new AlertMessageBox("失败", "手动执行任务失败");
								alert.show();
							};
						});
					}
				}
			});
			box.show();
		}
	});
	private TextButton recover=new TextButton("手动恢复",new SelectHandler() {
		@Override
		public void onSelect(SelectEvent event) {
			ConfirmMessageBox box=new ConfirmMessageBox("恢复任务", "你确认恢复该任务吗?这将引起依赖任务的连锁反应!");
			box.addHideHandler(new HideHandler() {
				@Override
				public void onHide(HideEvent event) {
					Dialog btn=(Dialog)event.getSource();
					if(btn.getHideButton().getText().equalsIgnoreCase("yes")){
						final AutoProgressMessageBox wait=new AutoProgressMessageBox("运行中","doing");
						wait.auto();
						wait.show();
						RPCS.getJobService().run(presenter.getJobModel().getId(), 2, new AbstractAsyncCallback<Void>() {
							@Override
							public void onSuccess(Void result) {
								wait.hide();
								Info.display("成功", "手动恢复任务开始");
							}
							@Override
							public void onFailure(Throwable caught) {
								wait.hide();
								AlertMessageBox alert=new AlertMessageBox("失败", "手动恢复任务失败");
								alert.show();
							};
						});
					}
				}
			});
			box.show();
		}
	});
	private TextButton onoff=new TextButton("开启/关闭",new SelectHandler() {
		@Override
		public void onSelect(SelectEvent event) {
			Boolean auto=presenter.getJobModel().getAuto();
			RPCS.getJobService().switchAuto(presenter.getJobModel().getId(), !auto, new AbstractAsyncCallback<Void>() {
				@Override
				public void onSuccess(Void result) {
					final ProgressMessageBox box=new ProgressMessageBox("开关 自动调度", "正在配置自动调度");
					box.setProgressText("doing...");
					box.show();
					RPCS.getJobService().getUpstreamJob(presenter.getJobModel().getId(), new AbstractAsyncCallback<JobModel>() {
						@Override
						public void onSuccess(JobModel result) {
							presenter.display(result);
							box.hide();
						}
						@Override
						public void onFailure(Throwable caught) {
							box.hide();
							super.onFailure(caught);
						};
					});
					Info.display("成功", "开启/关闭 自动调度 成功");
				}
			});
		}
	});
	private TextButton toDoc=new TextButton("转到开发中心",new SelectHandler() {
        @Override
        public void onSelect(SelectEvent event) {
            if(script!=null){
                String docId = ToolUtil.extractSyncFromId(script.getValue());
                if(docId!=null){
                    PlatformPlace pp=new PlacePath().toApp(App.Document).toDocType(DocType.MyDoc).toDocId(docId).create();
                    History.newItem(pp.getToken());
                }
            }
        }
    });
	
	private FieldSet baseFieldSet;
	private FieldSet hiveProcesserFieldSet;
	private FieldSet configFieldSet;
	private FieldSet scriptFieldSet;
	private FieldSet configParentField;
	private FieldSet resourceField;
	private FieldSet resourceParentField;
	
	private FieldLabel baseId;
	private FieldLabel baseName;
	private FieldLabel baseOwner;
	private FieldLabel baseDesc;
	private FieldLabel baseFollers;
	private FieldLabel baseAdmins;
	private FieldLabel baseJobType;
	private FieldLabel baseScheduleType;
	private FieldLabel baseCron;
	private FieldLabel baseAuto;
	private FieldLabel baseDepJobs;
	private FieldLabel baseDepCycle;
	private FieldLabel timezone;
	private FieldLabel offRaw;
	private FieldLabel jobCycle;
	private FieldLabel host;
	
	private HTMLPanel configContent;
	private HTMLPanel configParentContent;
	private CodeMirror script;
	private HTMLPanel resourceContent;
	private HTMLPanel resourceParentContent;
	
	private JobPresenter presenter;
	
	public CardInfo(JobPresenter presenter){
		this.presenter=presenter;
		addButton(history);
		addButton(depGraph);
		addButton(deleteGroup);
		addButton(editGroup);
		addButton(execute);
		addButton(recover);
		addButton(onoff);
		addButton(deleteGroup);
		addButton(configAdmin);
		addButton(toDoc);
		
		FlowLayoutContainer centerContainer=new FlowLayoutContainer();
		centerContainer.add(getBaseFieldSet(),new MarginData(3));
		//centerContainer.add(getHiveProcesserFieldSet(),new MarginData(3));
		centerContainer.add(getConfigFieldSet(),new MarginData(3));
		centerContainer.add(getScriptFieldSet(),new MarginData(3));
		centerContainer.add(getResourceField(),new MarginData(3));
		centerContainer.add(getConfigParentField(),new MarginData(3));
		centerContainer.add(getResourceParentField(),new MarginData(3));
		centerContainer.setScrollMode(ScrollMode.AUTOY);
		setCenter(centerContainer);
	}
	public static final String JAVA_MAIN_KEY="java.main.class";
	public static final String DEPENDENCY_CYCLE="zeus.dependency.cycle";
	private void display(final JobModel model){
		((Label)baseId.getWidget()).setText(model.getId());
		((Label)baseName.getWidget()).setText(model.getName());
		((Label)baseOwner.getWidget()).setText(model.getOwnerName());
		((Label)baseDesc.getWidget()).setText(model.getDesc());
		((Label)baseJobType.getWidget()).setText(model.getJobRunType());
		((Label)baseScheduleType.getWidget()).setText(model.getJobScheduleType());
		((Label)baseCron.getWidget()).setText(model.getCronExpression());
		((Label)timezone.getWidget()).setText(model.getDefaultTZ());
		((Label)host.getWidget()).setText(model.getHost());
		((Label)offRaw.getWidget()).setText(model.getOffRaw());
		if(model.getJobCycle().equals("day")){
			((Label)jobCycle.getWidget()).setText("天任务");
		}
		if(model.getJobCycle().equals("hour")){
			((Label)jobCycle.getWidget()).setText("小时任务");
		}
		
		if(model.getAuto()){
			((Label)baseAuto.getWidget()).setText("开启");
			((Label)baseAuto.getWidget()).getElement().setAttribute("style", "font-weight:bold;color:green");
		}else{
			((Label)baseAuto.getWidget()).setText("关闭");
			((Label)baseAuto.getWidget()).getElement().setAttribute("style", "font-weight:bold;color:red");
		}
		((Label)baseDepJobs.getWidget()).setText(model.getDependencies().toString());
		if("sameday".equals(model.getLocalProperties().get(DEPENDENCY_CYCLE))){
			((Label)baseDepCycle.getWidget()).setText("同一天");
		}else{
			((Label)baseDepCycle.getWidget()).setText("没有限制");
		}
		((Label)baseFollers.getWidget()).setText(model.getFollows().toString());
		((Label)baseAdmins.getWidget()).setText(model.getAdmins().toString());
		
		if(JobModel.DEPEND_JOB.equals(model.getJobScheduleType())){
			baseDepJobs.show();
			baseDepCycle.show();
			baseCron.hide();
			timezone.hide();
			offRaw.hide();
			jobCycle.hide();
			host.show();
		}
		if(JobModel.INDEPEN_JOB.equals(model.getJobScheduleType())){
			baseDepJobs.hide();
			baseDepCycle.hide();
			baseCron.show();
			timezone.hide();
			offRaw.hide();
			jobCycle.hide();
			host.show();
		}
		if(JobModel.CYCLE_JOB.equals(model.getJobScheduleType())){
			baseDepCycle.hide();
			baseCron.hide();
			timezone.show();
			offRaw.show();
			jobCycle.show();
			baseDepJobs.show();
			host.show();
		}
		
		//初始化辅助功能配置
		/*outputTableLabel.hide();
		keepDaysLabel.hide();
		driftPercentLabel.hide();
		syncTableLabel.hide();
		zkHostLabel.hide();
		zkPathLabel.hide();*/
		//getHiveProcesserFieldSet().hide();
		/*for (String post : model.getPostProcessers()) {
			if (post != null) {
				ProcesserType p = ProcesserType.parse(post);
				if (p != null) {
					if (p.getId().equalsIgnoreCase("hive")) {
						HiveP hiveP = (HiveP) p;
						((Label)outputTableLabel.getWidget()).setText(hiveP.getOutputTables());
						((Label)keepDaysLabel.getWidget()).setText(hiveP.getKeepDays());
						((Label)driftPercentLabel.getWidget()).setText(hiveP.getDriftPercent());
						((Label)syncTableLabel.getWidget()).setText(hiveP.getSyncTables());
						if(hiveP.getOutputTables()!=null&&!hiveP.getOutputTables().isEmpty()) {
							outputTableLabel.show();
						}
						if(hiveP.getKeepDays()!=null&&!hiveP.getKeepDays().isEmpty()) {
							keepDaysLabel.show();
						}
						if(hiveP.getDriftPercent()!=null&&!hiveP.getDriftPercent().isEmpty()) {
							driftPercentLabel.show();
						}
						if(hiveP.getSyncTables()!=null&&!hiveP.getSyncTables().isEmpty()) {
							syncTableLabel.show();
						}
						getHiveProcesserFieldSet().show();
					} else if (p.getId().equalsIgnoreCase("zookeeper")) {
						ZooKeeperP zp = (ZooKeeperP) p;
						if(zp.getUseDefault()) {
							continue;
						}
						((Label)zkHostLabel.getWidget()).setText(zp.getHost());
						((Label)zkPathLabel.getWidget()).setText(zp.getPath());
						zkHostLabel.show();
						zkPathLabel.show();
						getHiveProcesserFieldSet().show();
					}
				}
			}
		}*/
		
		Map<String, String> config=model.getLocalProperties();
		StringBuffer sb=new StringBuffer("<div style='font-size:13px'>");
		for(String key:config.keySet()){
			if(key.startsWith("secret.")){
				sb.append(key+"=*<br/>");
			}else if(key.equals("hadoop.hadoop.job.ugi")){
				String value=config.get(key);
				if(value.indexOf("#")!=-1){
					value=value.substring(0, value.indexOf("#"))+"*";
				}
				sb.append(key+"="+value+"<br/>");
			}else{
				sb.append(key+"="+config.get(key)+"<br/>");
			}
		}
		sb.append("</div>");
		configContent.getElement().setInnerHTML(sb.toString());
		
		Map<String, String> configAll=model.getAllProperties();
		sb=new StringBuffer("<div style='font-size:13px'>");
		for(String key:configAll.keySet()){
			if(config.get(key)==null){
				sb.append(key+"="+configAll.get(key)+"<br/>");
			}
		}
		sb.append("</div>");
		configParentContent.getElement().setInnerHTML(sb.toString());
		
		
		sb=new StringBuffer("<div style='font-size:13px'>");
		for(Map<String, String> record:model.getLocalResources()){
			sb.append("<p title='"+record.get("uri")+"'>"+record.get("name")+"</p>");
		}
		sb.append("</div>");
		resourceContent.getElement().setInnerHTML(sb.toString());
		
		sb=new StringBuffer("<div style='font-size:13px'>");
		for(Map<String, String> record:model.getAllResources()){
			if(!model.getLocalResources().contains(record)){
				sb.append("<p title='"+record.get("uri")+"'>"+record.get("name")+"</p>");
			}
		}
		sb.append("</div>");
		resourceParentContent.getElement().setInnerHTML(sb.toString());
		
		if(JobModel.MapReduce.equals(model.getJobRunType())){
			scriptFieldSet.hide();
		}else{
			script.setValue(model.getScript());
			scriptFieldSet.show();
		}
        if (!model.getJobRunType().equals(JobModel.MapReduce)
                && script != null
                && ToolUtil.extractSyncFromId(script.getValue()) != null
                && model.getOwner().equalsIgnoreCase(
                        presenter.getPlatformContext().getUser().getUid())) {
            toDoc.show();
        } else {
            toDoc.hide();
        }
	}

	public FieldSet getBaseFieldSet() {
		if(baseFieldSet==null){
			baseFieldSet=new FieldSet();
			baseFieldSet.setHeadingText("基本信息");
			baseFieldSet.setHeight(220);
			
			HorizontalLayoutContainer layoutContainer=new HorizontalLayoutContainer();
			baseFieldSet.add(layoutContainer);
			
			VerticalLayoutContainer leftContainer = new VerticalLayoutContainer();
			leftContainer.setWidth(300);
			VerticalLayoutContainer rightContainer=new VerticalLayoutContainer();
			rightContainer.setWidth(300);
			layoutContainer.add(leftContainer,new HorizontalLayoutData(-1,1));
			layoutContainer.add(rightContainer,new HorizontalLayoutData(-1,1));
			
			baseId=new FieldLabel(getLabel(),"id");
			baseName=new FieldLabel(getLabel(),"名称");
			baseOwner=new FieldLabel(getLabel(),"所有人");
			baseDesc=new FieldLabel(new Label("",true),"描述");
			baseJobType=new FieldLabel(getLabel(),"任务类型");
			baseScheduleType=new FieldLabel(new Label(),"调度类型");
			baseCron=new FieldLabel(getLabel(),"定时表达式");
			baseAuto=new FieldLabel(getLabel(),"自动调度");
			timezone=new FieldLabel(getLabel(),"任务时区");
			offRaw=new FieldLabel(getLabel(),"启动延时");
			jobCycle=new FieldLabel(getLabel(),"任务周期");
			baseDepJobs=new FieldLabel(getLabel(),"依赖任务");
			baseDepCycle=new FieldLabel(getLabel(),"依赖周期");
			baseFollers=new FieldLabel(getLabel(),"关注人员");
			baseAdmins=new FieldLabel(getLabel(),"管理员");
			host=new FieldLabel(getLabel(),"Host");
			
			leftContainer.add(baseId,new VerticalLayoutContainer.VerticalLayoutData(1, -1));
			leftContainer.add(baseName,new VerticalLayoutContainer.VerticalLayoutData(1, -1));
			leftContainer.add(baseOwner,new VerticalLayoutContainer.VerticalLayoutData(1, -1));
			leftContainer.add(baseDesc,new VerticalLayoutContainer.VerticalLayoutData(1, -1));
			rightContainer.add(baseJobType,new VerticalLayoutContainer.VerticalLayoutData(1, -1));
			rightContainer.add(baseScheduleType,new VerticalLayoutContainer.VerticalLayoutData(1, -1));
			rightContainer.add(baseCron,new VerticalLayoutContainer.VerticalLayoutData(1, -1));
			rightContainer.add(baseAuto,new VerticalLayoutContainer.VerticalLayoutData(1, -1));
			rightContainer.add(baseDepJobs,new VerticalLayoutContainer.VerticalLayoutData(1, -1));
			rightContainer.add(baseDepCycle,new VerticalLayoutContainer.VerticalLayoutData(1, -1));
			rightContainer.add(timezone,new VerticalLayoutContainer.VerticalLayoutData(1, -1));
			rightContainer.add(offRaw,new VerticalLayoutContainer.VerticalLayoutData(1, -1));
			rightContainer.add(jobCycle,new VerticalLayoutContainer.VerticalLayoutData(1, -1));
			rightContainer.add(host,new VerticalLayoutContainer.VerticalLayoutData(1, -1));
			leftContainer.add(baseFollers,new VerticalLayoutContainer.VerticalLayoutData(1, -1));
			leftContainer.add(baseAdmins,new VerticalLayoutContainer.VerticalLayoutData(1, -1));
			//leftContainer.add(host,new VerticalLayoutContainer.VerticalLayoutData(1, -1));

		}
		return baseFieldSet;
	}

	/*private FieldLabel outputTableLabel;
	private FieldLabel syncTableLabel;
	private FieldLabel keepDaysLabel;
	private FieldLabel driftPercentLabel;
	private FieldLabel zkHostLabel;
	private FieldLabel zkPathLabel;*/
	
	private Label getLabel(){
		Label label = new Label("",false);
		label.getElement().getStyle().setPaddingTop(3, Unit.PX);
		return label;
	}
	private FieldSet getHiveProcesserFieldSet() {
		if (hiveProcesserFieldSet == null) {
			hiveProcesserFieldSet = new FieldSet();
			hiveProcesserFieldSet.setCollapsible(true);
			hiveProcesserFieldSet.setHeadingText("辅助功能配置");
			VerticalLayoutContainer container = new VerticalLayoutContainer();
			/*outputTableLabel = new FieldLabel(getLabel(),"产出的表名");
			syncTableLabel = new FieldLabel(getLabel(),"阻塞同步的天网表");
			keepDaysLabel = new FieldLabel(getLabel(),"旧分区保留天数");
			driftPercentLabel = new FieldLabel(getLabel(),"产出数据浮动报警");
			zkHostLabel = new FieldLabel(getLabel(),"自定义ZK host");
			zkPathLabel = new FieldLabel(getLabel(),"自定义ZK path");
			
			container.add(syncTableLabel);
			container.add(outputTableLabel);
			container.add(keepDaysLabel);
			container.add(driftPercentLabel);
			container.add(zkHostLabel);
			container.add(zkPathLabel);*/

			hiveProcesserFieldSet.add(container);
		}
		return hiveProcesserFieldSet;
	}

	public FieldSet getConfigFieldSet() {
		if(configFieldSet==null){
			configFieldSet=new FieldSet();
			configFieldSet.setCollapsible(true);
			configFieldSet.setHeadingText("配置项信息");
			configContent=new HTMLPanel("");
			configFieldSet.add(configContent);
		}
		return configFieldSet;
	}

	public FieldSet getConfigParentField() {
		if(configParentField==null){
			configParentField=new FieldSet();
			configParentField.setCollapsible(true);
			configParentField.setHeadingText("继承的配置项信息");
			
			configParentContent=new HTMLPanel("");
			configParentField.add(configParentContent);
		}
		return configParentField;
	}

	public FieldSet getResourceField() {
		if(resourceField==null){
			resourceField=new FieldSet();
			resourceField.setCollapsible(true);
			resourceField.setHeadingText("资源信息");
			
			resourceContent=new HTMLPanel("");
			resourceField.add(resourceContent);
		}
		return resourceField;
	}

	public FieldSet getResourceParentField() {
		if(resourceParentField==null){
			resourceParentField=new FieldSet();
			resourceParentField.setCollapsible(true);
			resourceParentField.setHeadingText("继承的资源信息");
			resourceParentContent=new HTMLPanel("");
			resourceParentField.add(resourceParentContent);
		}
		return resourceParentField;
	}
	@Override
	public void refresh(JobModel t) {
		display(t);
	}

	public FieldSet getScriptFieldSet() {
		if(scriptFieldSet==null){
			scriptFieldSet=new FieldSet();
			scriptFieldSet.setCollapsible(true);
			scriptFieldSet.setHeadingText("脚本");
			CodeMirrorConfig cmc=new CodeMirrorConfig();
			script=new CodeMirror(cmc);
			scriptFieldSet.add(script);
			scriptFieldSet.setWidth("96%");
			scriptFieldSet.addBeforeHideHandler(new BeforeHideEvent.BeforeHideHandler() {
				@Override
				public void onBeforeHide(BeforeHideEvent event) {
					if(scriptFieldSet.getElement().getWidth(false)==0) {
						event.setCancelled(true);
					}
				}
			});
		}
		return scriptFieldSet;
	}
}
