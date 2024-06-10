package com.traps.RoshanNOCTraps.traps.hw;

import com.traps.RoshanNOCTraps.db.DbOperation;
import com.traps.RoshanNOCTraps.db.HwDao;
import com.traps.RoshanNOCTraps.db.KafkaOperation;
import com.traps.RoshanNOCTraps.db.ZteDoa;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.smi.OID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class ProcessHwPdu {


//    private HwDao hwDao;
//    @Autowired
//    public ProcessHwPdu(HwDao hwDao){
//        this.hwDao = hwDao;
//
//    }



    // Inside your method
// Inside your method
    List<Long> alarmIdList = Arrays.asList(
            21807L, 22214L, 65080L, 65070L, 65501L,
            65033L, 65381L, 29201L, 25622L, 65084L,
            65067L, 5700L, 65081L, 25621L, 65068L,
            65502L, 65059L, 65071L, 21825L, 65069L
    );




    public void processPdu(CommandResponderEvent crEvent) throws SQLException {

        PDU pdu = crEvent.getPDU();
        processHwPDU(pdu);

    }


    private void processHwPDU(PDU pdu) throws SQLException {


        if (pdu.getType() == PDU.TRAP) {
            String intendedAlarmHwString = pdu.getVariable(new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.9.0")).toString();
            Long intendedAlarmHuawei = Long.parseLong(intendedAlarmHwString);

            if (alarmIdList.contains(intendedAlarmHuawei)) {
                filterHuaweiTrap(pdu, intendedAlarmHuawei);
            }
        }
    }

    private void filterHuaweiTrap(PDU pdu,Long intendedAlarmHuawei) throws SQLException {


        HwTrapBody hwTrapBody = new HwTrapBody();
        hwTrapBody.setTrapId(pdu.getVariable(new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.1.0")).toString());
        hwTrapBody.setAlarmClearedTime(pdu.getVariable(new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.15.0")).toString());


        String clearOrNot = pdu.getVariable(new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.12.0")).toString();

        //1 = cleared
        //2 arrival
//        System.out.println("alarm_clear_time_hw: "+hwTrapBody.getAlarmClearedTime() +"   =====> Is Clear Or Not:  "+clearOrNot);

        if (hwTrapBody.getAlarmClearedTime() == null || hwTrapBody.getAlarmClearedTime().isBlank() || hwTrapBody.getAlarmClearedTime().isEmpty()) {
            //site identification section

            hwTrapBody.setNewOrClear(1L);
            hwTrapBody.setSiteName(pdu.getVariable(new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.4.0")).toString());

            String objectInstanceName_hw = pdu.getVariable(new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.27.0")).toString();
            //event time section
            hwTrapBody.setAlarmArrivalTime(pdu.getVariable(new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.3.0")).toString());

            //alarm identification
            hwTrapBody.setAlarmCode(intendedAlarmHuawei.toString());
            hwTrapBody.setAlarmName(pdu.getVariable(new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.28.0")).toString());
           hwTrapBody.setAlarmEventType(pdu.getVariable(new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.10.0")).toLong());
           hwTrapBody.setAlarmNetType(pdu.getVariable(new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.6.0")).toString());

            hwTrapBody.setAlarmSeverity(pdu.getVariable(new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.11.0")).toLong());

            //set up site info
            hwTrapBody = extractSiteInfoHW(objectInstanceName_hw.trim(),hwTrapBody);

            //setup site id
            hwTrapBody.setSiteId(setUpSiteId(hwTrapBody.getSiteId()));

            //setup service type
            hwTrapBody.setAlarmServiceType(setUpServiceType(hwTrapBody.getSiteId(),hwTrapBody.getAlarmCode()));

            //setup display site id
            hwTrapBody.setDisplaySiteId(setUpDisplaySiteId(hwTrapBody.getSiteId(),hwTrapBody.getAlarmServiceType()));

//            System.out.println("HW trap INSERT: ");


//            produce to kafka
            KafkaOperation.sendHwTrap(hwTrapBody);
//            DbOperation.addHwTrap(hwTrapBody);
//            this.saveOrUpdateDatabaseHW("insert",pdu);
        }else{

            hwTrapBody.setNewOrClear(2L);
//            produce to kafka
            KafkaOperation.sendHwTrap(hwTrapBody);

//            System.out.println("HW trap UPDATE: "+hwTrapBody);
//            DbOperation.updateHwTrap(hwTrapBody.getTrapId(), hwTrapBody);
//            this.saveOrUpdateDatabaseHW("update",pdu);
        }
    }


    private HwTrapBody extractSiteInfoHW(String info,HwTrapBody hwTrapBody) {

        String siteId = null;
        String siteName = hwTrapBody.getSiteName().trim();
        String alarmCodeHW = hwTrapBody.getAlarmCode();

//        same condition 21801
        if(alarmCodeHW.equals("22214")){

            String arr[] = info.split(",");
            String term = arr[4].trim();
            siteName = term.substring(term.indexOf("=")+1);
            siteId = siteName.substring(0,siteName.indexOf("_")).trim();

        }
        else
        {
            String version_1_from_field_4 = siteName.substring(0, siteName.indexOf("_")).trim();
            String version_2_from_field_4 = siteName.substring(siteName.indexOf("-")+1,siteName.indexOf("_")).trim();
            String version_3_from_field_4 = siteName.substring(0, siteName.indexOf("_UL_") + 3).trim();

            if(alarmCodeHW.equals("25621") || alarmCodeHW.equals("29201") || alarmCodeHW.equals("65059")){

                if(siteName.indexOf("_UL_") != -1){
                    siteId = version_3_from_field_4;
                }else if(siteName.indexOf("-") != -1){
                    siteId = version_2_from_field_4;
                }else{
                    siteId = version_1_from_field_4;
                }
            }
            else if(alarmCodeHW.equals("65084") || alarmCodeHW.equals("65069") || alarmCodeHW.equals("65081")  || alarmCodeHW.equals("65068") || alarmCodeHW.equals("65080")){

                if(siteName.indexOf("_UL_") != -1){
                    siteId = version_3_from_field_4;
                }else{
                    if(siteName.indexOf("-") != -1){
                        siteId = version_2_from_field_4;
                    }else{
                        siteId = version_1_from_field_4;
                    }

                }

            }
            else{
                if(alarmCodeHW.equals("65033")  || alarmCodeHW.equals("65501") || alarmCodeHW.equals("21807")||
                        alarmCodeHW.equals("65070") || alarmCodeHW.equals("5700")  ||  alarmCodeHW.equals("65071") ){
                    siteId = version_2_from_field_4;
                    siteName = siteName;
                }
                else if( alarmCodeHW.equals("25622")){
//                    same condition alarmCodeHW.equals("22202") ||
                    String arr[] = info.split(",");
                    String term = arr[7].trim();
                    siteName = term.substring(term.indexOf("=")+1);
                    siteId = siteName.substring(0,siteName.indexOf("_")).trim();

                }
                else if(alarmCodeHW.equals("65067")   || alarmCodeHW.equals("21825") || alarmCodeHW.equals("65502")){

                    if(siteName.indexOf("-") != -1){
                        siteId = version_2_from_field_4;
                    }else{
                        siteId = version_1_from_field_4;
                    }
                }
                else if(siteId == null || siteId.isEmpty()){
                    siteId = "RANDOM";
                }

            }
        }

        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);

        return hwTrapBody;

//        alarmServiceTypeHW = pduProccessConfig.setUpServiceType(site_id_hw,alarmCodeHW,"HW");
//
//        site_id_hw = pduProccessConfig.setUpSiteId(site_id_hw,alarmCodeHW,"HW");
//
//        displaySiteIdHW = pduProccessConfig.setupDisplaySiteId(site_id_hw,alarmServiceTypeHW,"HW");


    }


    public String setUpServiceType(String siteId, String alarmCodeString) {

        String localServiceType;
        Long alarmCode = Long.parseLong(alarmCodeString);

        List<String> alarmCodes3G = Arrays.asList(
                "22202", "65067", "65080", "198083022", "199083022",
                "65068", "65381", "198083023", "199083023", "200083022", "200083023"
        );

        List<String> alarmCodes4G = Arrays.asList(
                "65081", "29201", "198094419", "198094461",
                "65084", "198092295", "198094422"
        );

        if(alarmCode == 22214L){
            localServiceType = "3G";
        }
        else if (alarmCodes3G.contains(alarmCode)) {
            localServiceType = "3G";
        } else if (alarmCodes4G.contains(alarmCode)) {
            localServiceType = "4G";
        }
        else if(alarmCode == 65069L){

            if(siteId.endsWith("_UL")){
                localServiceType = "4G";
            }else{
                localServiceType = "3G";
            }
        }
        else{
            localServiceType = "2G";
        }

        return localServiceType;

    }

    public String setUpDisplaySiteId(String tempSiteId, String temServiceType) {

        String localDisplaySiteId;

        if(temServiceType.equalsIgnoreCase("3G")){
            if(tempSiteId.charAt(3) == 'M'){
                localDisplaySiteId =    tempSiteId.substring(0,4).concat("U").concat(tempSiteId.substring(4));
            }else{
                localDisplaySiteId =    tempSiteId.substring(0,3).concat("U").concat(tempSiteId.substring(3));
            }
        }
        else if(temServiceType.equalsIgnoreCase("4G")){
            localDisplaySiteId = tempSiteId.concat("_UL");
        }else{
            localDisplaySiteId = tempSiteId;
        }

        return localDisplaySiteId;
    }


    public String setUpSiteId(String siteId) {


        String inputString = siteId;
        String result = "";

        if(inputString == null || inputString.equalsIgnoreCase("RANDOM")){
            result = siteId;
        }

        else{
            if (inputString.length() >= 5) {
                char charAt3 = inputString.charAt(3);
                char charAt4 = inputString.charAt(4);

                if(charAt3 == 'M'){
                    if (charAt4 == 'U' || charAt4 == 'P' || charAt4 == 'C'  || charAt4 == 'L' || charAt4 == 'M') {
                        result = inputString.substring(0, 4) + inputString.substring(5, 8); //kblm + 100
                    }else {
                        result = inputString.substring(0, 4) + inputString.substring(4, 7); //KBLM +100
                    }

                }
                else {
                    if (charAt3 == 'U' || charAt3 == 'P' || charAt3 == 'C' || charAt3 == 'L') {

                        if (charAt4 == 'U' || charAt4 == 'P' || charAt4 == 'C'  || charAt4 == 'L' || charAt4 == 'M') {
                            result = inputString.substring(0, 3) + inputString.substring(5, 8);
                        }else{
                            result = inputString.substring(0, 3) + inputString.substring(4, 7);
                        }
                    }
                    else {
                        result = inputString.substring(0, 3) + inputString.substring(3, 6); //ok
                    }
                }
            }
        }


        return result;
    }


}
