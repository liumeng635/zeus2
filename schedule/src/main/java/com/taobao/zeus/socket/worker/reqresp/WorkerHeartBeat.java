package com.taobao.zeus.socket.worker.reqresp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.UUID;

import org.jboss.netty.channel.ChannelFuture;

import com.taobao.zeus.jobs.JobContext;
import com.taobao.zeus.jobs.sub.tool.MemUseRateJob;
import com.taobao.zeus.schedule.mvc.ScheduleInfoLog;
import com.taobao.zeus.socket.master.AtomicIncrease;
import com.taobao.zeus.socket.protocol.Protocol.HeartBeatMessage;
import com.taobao.zeus.socket.protocol.Protocol.Operate;
import com.taobao.zeus.socket.protocol.Protocol.Request;
import com.taobao.zeus.socket.protocol.Protocol.SocketMessage;
import com.taobao.zeus.socket.protocol.Protocol.SocketMessage.Kind;
import com.taobao.zeus.socket.worker.WorkerContext;

public class WorkerHeartBeat {
	public static String host = UUID.randomUUID().toString();
	static {
		try {
			host = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			// ignore
		}
	}

	public ChannelFuture execute(WorkerContext context) {
		JobContext jobContext = JobContext.getTempJobContext();
		MemUseRateJob job = new MemUseRateJob(jobContext, 1);
		try {
			int exitCode = -1;
			int count = 0;
			while (count < 3 && exitCode != 0) {
				count++;
				exitCode = job.run();
			}
			if (exitCode != 0) {
				ScheduleInfoLog.error("HeartBeat Shell Error", new Exception(
						jobContext.getJobHistory().getLog().getContent()));
				// 防止后面NPE
				jobContext.putData("mem", 1.0);
			}
		} catch (Exception e) {
			ScheduleInfoLog.error("memratejob", e);
		}
		HeartBeatMessage hbm = HeartBeatMessage.newBuilder()
				.setMemRate(((Double) jobContext.getData("mem")).floatValue())
				.addAllDebugRunnings(context.getDebugRunnings().keySet())
				.addAllManualRunnings(context.getManualRunnings().keySet())
				.addAllRunnings(context.getRunnings().keySet())
				.setTimestamp(new Date().getTime()).setHost(host).build();
		Request req = Request.newBuilder()
				.setRid(AtomicIncrease.getAndIncrement())
				.setOperate(Operate.HeartBeat).setBody(hbm.toByteString())
				.build();

		SocketMessage sm = SocketMessage.newBuilder().setKind(Kind.REQUEST)
				.setBody(req.toByteString()).build();
		return context.getServerChannel().write(sm);
	}
}
