package com.traps.RoshanNOCTraps;

import com.traps.RoshanNOCTraps.traps.hw.HwTraps;
import com.traps.RoshanNOCTraps.traps.txn.TxnHwTraps;
import com.traps.RoshanNOCTraps.traps.zte.ZteTraps;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RoshanNocTrapsManagementApplication {


	public static void main(String[] args) {

		SpringApplication.run(RoshanNocTrapsManagementApplication.class, args);
		new ZteTraps().run();
		new HwTraps().run();

//		new TxnHwTraps().run();


	}

}




