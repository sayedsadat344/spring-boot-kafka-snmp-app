package com.traps.RoshanNOCTraps.traps.zte;

import com.mycompany.app.sharedClasses.HwTrapBody;
import com.mycompany.app.sharedClasses.ZteTrapBody;
import com.traps.RoshanNOCTraps.db.DbOperation;
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
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class ProcessZtePdu {

//
//    private final ZteDoa zteDoa;
//    @Autowired
//    public ProcessZtePdu(ZteDoa zteDoa){
//        this.zteDoa = zteDoa;
//    }


    private List<Long> alarmValues = Arrays.asList(
            199087337L, 198092550L, 198087337L, 198092295L,
            198083023L, 199083023L, 198092562L, 198094422L, 198092559L
    );

    private static final String FILE_PATH = "zte-output.txt";

    public void processPdu(CommandResponderEvent crEvent) throws SQLException {

        PDU pdu = crEvent.getPDU();
        processZTEPDU(pdu);

    }


    private void processZTEPDU(PDU pdu) throws SQLException {


        if (pdu.getType() == PDU.TRAP) {
            String intendedAlarmZteString = pdu.getVariable(new OID("1.3.6.1.4.1.3902.4101.1.3.1.11")).toString();
            Long intendedAlarmZte = Long.parseLong(intendedAlarmZteString);
            if (alarmValues.contains(intendedAlarmZte)) {
                filterZteTrap(pdu);
            }
        }
    }

    private void filterZteTrap(PDU pdu) throws SQLException {

        ZteTrapBody zteTrapBody = new ZteTrapBody();
        zteTrapBody.setTrapId(pdu.getVariable(new OID("1.3.6.1.4.1.3902.4101.1.3.1.24")).toString());
        zteTrapBody.setAlarmCode(pdu.getVariable(new OID("1.3.6.1.4.1.3902.4101.1.3.1.11")).toString());
        String eventTime = pdu.getVariable(new OID("1.3.6.1.4.1.3902.4101.1.3.1.3")).toString();
        String alarmNewOrClear = pdu.getVariable(new OID("1.3.6.1.6.3.1.1.4.1.0")).toString();

        if (alarmNewOrClear.equals("1.3.6.1.4.1.3902.4101.1.4.1.1")) {
            zteTrapBody.setAlarmArrivalTime(eventTime);
            zteTrapBody.setAlarmName(pdu.getVariable(new OID("1.3.6.1.4.1.3902.4101.1.3.1.14")).toString());
            zteTrapBody.setSiteName(pdu.getVariable(new OID("1.3.6.1.4.1.3902.4101.1.3.1.26")).toString());

            String localRNCId = pdu.getVariable(new OID("1.3.6.1.4.1.3902.4101.1.3.1.15")).toString();
            String objectInstanceName_zte = pdu.getVariable(new OID("1.3.6.1.4.1.3902.4101.1.3.1.8")).toString();
            //extract site info
            zteTrapBody = extractSiteInfoZTE(zteTrapBody,objectInstanceName_zte,localRNCId);

            //setup site id
            zteTrapBody.setSiteId(setUpSiteId(zteTrapBody.getSiteId()));

            //setup service type
            zteTrapBody.setAlarmServiceType(setUpServiceType(zteTrapBody.getSiteId(),zteTrapBody.getAlarmCode()));

            //setup display site id
            zteTrapBody.setDisplaySiteId(setUpDisplaySiteId(zteTrapBody.getSiteId(),zteTrapBody.getAlarmServiceType()));

            //other details
            zteTrapBody.setAlarmEventType(pdu.getVariable(new OID("1.3.6.1.4.1.3902.4101.1.3.1.4")).toLong());


//            System.out.println("ZTE trap INSERT: "+zteTrapBody);
            zteTrapBody.setNewOrClear(1L);

            zteTrapBody.setId(DbOperation.generateUniqueId());
//            kafka send
            KafkaOperation.sendZteTrap(zteTrapBody);
            ///DATABASE CONNECTIVITY ////
//            saveOrUpdateDatabaseZTE("insert",pdu);
//            DbOperation.addZteTrap(zteTrapBody);

        }
        else if (alarmNewOrClear.equals("1.3.6.1.4.1.3902.4101.1.4.1.2")) {
            zteTrapBody.setAlarmClearedTime(eventTime);

//            System.out.println("ZTE trap UPDATE: "+zteTrapBody);

            zteTrapBody.setNewOrClear(2L);

            KafkaOperation.sendZteTrap(zteTrapBody);

//            DbOperation.updateZteTrap(zteTrapBody.getTrapId(),zteTrapBody);
            ///DATABASE CONNECTIVITY ////
//            saveOrUpdateDatabaseZTE("update",pdu);
        }
        appendData(zteTrapBody);
    }

    private void appendData(ZteTrapBody zte) {
        try {
            Files.write(Paths.get(FILE_PATH), (zte.toString() + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.println("Object written to file successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ZteTrapBody extractSiteInfoZTE(ZteTrapBody zteTrapBody, String objectInstanceName_zte, String localRNCId) {

    String alarmCode = zteTrapBody.getAlarmCode();
    String nodeBId = null;
    String rncId = null;
    String siteId = null;
    String siteName = zteTrapBody.getSiteName();

        if(alarmCode.equals("199083023")){
            String arr[] = objectInstanceName_zte.split(";");
            nodeBId = arr[1].trim();
            nodeBId = nodeBId.substring(nodeBId.indexOf(":")+1).trim();
            String arr2[] = localRNCId.split(",");
            rncId = arr2[0].trim();
            rncId = rncId.substring(rncId.indexOf("(")+1,rncId.length()-1).trim();

            if(rncId.isEmpty()){
                if(siteName.equals("GDZRZ01(7)")){
                    rncId = "7";
                }
                if(siteName.equals("HRTZR01-RNC01(3)")){
                    rncId = "3";
                }
                if(siteName.equals("KDRZR01-RNC01(4)")){
                    rncId = "4";
                }
            }
        }

        else if(alarmCode.equals("198083023")){

            String arr3[] = objectInstanceName_zte.split(";");
            nodeBId = arr3[0];
            nodeBId = nodeBId.substring(nodeBId.indexOf(":")+1).trim();
            rncId = siteName.substring(siteName.indexOf("(")+1,siteName.length()-1).trim();
            if(rncId.isEmpty()){
                if(siteName.equals("GDZRZ01(7)")){
                    rncId = "7";
                }
                if(siteName.equals("HRTZR01-RNC01(3)")){
                    rncId = "3";
                }
                if(siteName.equals("KDRZR01-RNC01(4)")){
                    rncId = "4";
                }
            }

        }

        else {
            //HRT150_Char_Borjk_P2_Opex_QZ
            //case 1: P,C,U = 7 GDZC001_BSC_Site_Gardez001_P1(3201)
            String version_1_from_field_26_length_7 =  siteName.substring(0, 7).trim();
            if (siteName.charAt(3) == 'P' || siteName.charAt(3) == 'C' || siteName.charAt(3) == 'U') {
                //7
                siteId =version_1_from_field_26_length_7;
            } else if (siteName.charAt(3) == 'M') {
                if (siteName.charAt(4) == 'U') {
                    siteId = siteName.substring(0, 8).trim();
                }else if(siteName.charAt(6) == '('){
                    siteId = siteName.substring(0, 6).trim();
                }
                else {
                    //7
                    siteId = version_1_from_field_26_length_7;
                }
            } else if(siteName.charAt(3) == 'L') {
                //7
                siteId = version_1_from_field_26_length_7;

            }else if(siteName.indexOf("_UL_") != -1 ){
                //8
                siteId = siteName.substring(0, 9).trim();
            }else{
                //6
                siteId = siteName.substring(0, 6).trim();
            }
        }

        zteTrapBody.setSiteName(siteName);
        zteTrapBody.setSiteId(siteId);
        zteTrapBody.setAlarmNodeBId(nodeBId);
        zteTrapBody.setAlarmRncId(rncId);

        return zteTrapBody;

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
