package com.traps.RoshanNOCTraps;

import com.mycompany.app.sharedClasses.HwTrapBody;
import com.traps.RoshanNOCTraps.db.ZteDaoImpl;
import com.traps.RoshanNOCTraps.traps.hw.HwTraps;
import com.traps.RoshanNOCTraps.traps.hw.ProcessHwPdu;
import com.traps.RoshanNOCTraps.traps.zte.ProcessZtePdu;
import com.traps.RoshanNOCTraps.traps.zte.ZteTraps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RoshanNocTrapsManagementApplication {


	public static void main(String[] args) {



		SpringApplication.run(RoshanNocTrapsManagementApplication.class, args);
		new ZteTraps().run();
		new HwTraps().run();


	}

}
