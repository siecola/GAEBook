package com.siecola.exemplo1.cronservices;

import java.util.Calendar;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("cron1")
public class CronService {

	private static final Logger log = Logger.getLogger("CronService");

	@GET
	@Produces("application/json")
	@Path("testcron")
	public void testCron() {
		log.severe("Cron message --- " + Calendar.getInstance().getTime());
	}
}
