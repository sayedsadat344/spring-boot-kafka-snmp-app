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
            198083023L, 199083023L, 198092562L, 198094422L, 198092559L,198099803L,198200011L,198200001L,1014L,198094466L,
            198200004L
    );

//    198094466L

//    private List<Long> alarmValues = Arrays.asList(
//            199087337L
//    );


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

                System.out.println("TRAP: "+pdu);
                appendData(pdu,intendedAlarmZteString+".txt");
                    filterZteTrap(pdu);

            }
        }
    }

    private void filterZteTrap(PDU pdu) throws SQLException {

//        System.out.println("Trap -1 :");
        ZteTrapBody zteTrapBody = new ZteTrapBody();

        zteTrapBody.setTrapId(pdu.getVariable(new OID("1.3.6.1.4.1.3902.4101.1.3.1.24")).toString());
        zteTrapBody.setAlarmCode(pdu.getVariable(new OID("1.3.6.1.4.1.3902.4101.1.3.1.11")).toString());

        String eventTime = pdu.getVariable(new OID("1.3.6.1.4.1.3902.4101.1.3.1.3")).toString();
        String alarmNewOrClear = pdu.getVariable(new OID("1.3.6.1.6.3.1.1.4.1.0")).toString();
//        System.out.println("Trap -2 :");

        if (alarmNewOrClear.equals("1.3.6.1.4.1.3902.4101.1.4.1.1")) {
//            System.out.println("Trap -3 :");
            //new alarm
            zteTrapBody.setAlarmArrivalTime(eventTime);
            zteTrapBody.setAlarmName(pdu.getVariable(new OID("1.3.6.1.4.1.3902.4101.1.3.1.14")).toString());
            zteTrapBody.setSiteName(pdu.getVariable(new OID("1.3.6.1.4.1.3902.4101.1.3.1.26")).toString());

            String localRNCId = pdu.getVariable(new OID("1.3.6.1.4.1.3902.4101.1.3.1.15")).toString();
            String objectInstanceName_zte = pdu.getVariable(new OID("1.3.6.1.4.1.3902.4101.1.3.1.8")).toString();

//            System.out.println("Trap -4 :");
            //extract site info
            zteTrapBody = extractSiteInfoZTE(zteTrapBody,objectInstanceName_zte,localRNCId);

//            System.out.println("Trap -5 :");

            //setup site id
            zteTrapBody.setSiteId(setUpSiteId(zteTrapBody.getSiteId()));
//            System.out.println("Trap -6 :");

            //setup service type
            zteTrapBody.setAlarmServiceType(setUpServiceType(zteTrapBody.getSiteId(),zteTrapBody.getAlarmCode()));
//            System.out.println("Trap -7 :");

            //setup display site id
            zteTrapBody.setDisplaySiteId(setUpDisplaySiteId(zteTrapBody.getSiteId(),zteTrapBody.getAlarmServiceType(),zteTrapBody.getAlarmRncId(),zteTrapBody.getAlarmNodeBId()));
//            System.out.println("Trap -8 :");

            //other details
            zteTrapBody.setAlarmEventType(pdu.getVariable(new OID("1.3.6.1.4.1.3902.4101.1.3.1.4")).toLong());
//            System.out.println("Trap -9 :");

//            System.out.println("ZTE trap INSERT: "+zteTrapBody);
            zteTrapBody.setNewOrClear(1L);

            zteTrapBody.setId(DbOperation.generateUniqueId());
//            kafka send
//            KafkaOperation.sendZteTrap(zteTrapBody);



            ///DATABASE CONNECTIVITY ////
//            saveOrUpdateDatabaseZTE("insert",pdu);
//            DbOperation.addZteTrap(zteTrapBody);

        }
        else if (alarmNewOrClear.equals("1.3.6.1.4.1.3902.4101.1.4.1.2")) {

            //old alarm
            zteTrapBody.setAlarmClearedTime(eventTime);

            zteTrapBody.setNewOrClear(2L);

//            KafkaOperation.sendZteTrap(zteTrapBody);



//            DbOperation.updateZteTrap(zteTrapBody.getTrapId(),zteTrapBody);
            ///DATABASE CONNECTIVITY ////
//            saveOrUpdateDatabaseZTE("update",pdu);
        }

        System.out.println("\nZTE: "+zteTrapBody);
        System.out.println("*******************************************");
//        appendData(zteTrapBody);

//        if(zteTrapBody.getSiteId().equalsIgnoreCase("GZN012") || zteTrapBody.getSiteId().equalsIgnoreCase("GZNU012")
//        || zteTrapBody.getDisplaySiteId().equalsIgnoreCase("GZN012") || zteTrapBody.getDisplaySiteId().equalsIgnoreCase("GZNU012")){
//            appendData(zteTrapBody,"GZN012-body.txt");
//        }
//
//        if(zteTrapBody.getSiteId().equalsIgnoreCase("MZR159") || zteTrapBody.getSiteId().equalsIgnoreCase("MZRU159")
//                || zteTrapBody.getDisplaySiteId().equalsIgnoreCase("MZR159") || zteTrapBody.getDisplaySiteId().equalsIgnoreCase("MZRU159")){
//            appendData(zteTrapBody,"MZR159-body.txt");
//        }

        appendData(zteTrapBody,zteTrapBody.getAlarmCode()+"-body.txt");
    }




    private ZteTrapBody extractSiteInfoZTE(ZteTrapBody zteTrapBody, String objectInstanceName_zte, String localRNCId) {

    String alarmCode = zteTrapBody.getAlarmCode();
    String nodeBId = null;
    String rncId = null;
    String siteId = null;
    String siteName = zteTrapBody.getSiteName();



        if(alarmCode.equals("199087337") || alarmCode.equals("198087337")){

            //8 rnc and 15 obj

            String arr[] = localRNCId.split(",");

            siteName = arr[2].trim();

            siteId = siteName.substring(0, 7).trim();

            if(siteId.equalsIgnoreCase("GBtsEq")){
                System.out.println("Found: "+zteTrapBody.toString());
            }

        } else if (alarmCode.equals("199083023") || alarmCode.equals("198083023")) {
            String extractRncIdFromSiteName = siteName.substring(siteName.indexOf("(") + 1, siteName.length() - 1).trim();

            String arr[] = objectInstanceName_zte.split(";");

            if(alarmCode.equals("199083023")){
                nodeBId = arr[1].trim();
            }else{
                nodeBId = arr[0].trim();
            }

            nodeBId = nodeBId.substring(nodeBId.indexOf(":")+1).trim();

            // Extract RNC ID from Site Name
            rncId = extractRncIdFromSiteName;

            // If RNC ID is empty, assign a default value based on the site name
            if (rncId.isEmpty()) {
                rncId = getRncIdForSite(siteName);
            }



        } else if(alarmCode.equals("198092295") || alarmCode.equals("198092550") || alarmCode.equals("198092559") || alarmCode.equals("198092562")
                || alarmCode.equals("198094422") || alarmCode.equals("198099803") || alarmCode.equals("1014") || alarmCode.equals("198094466") || alarmCode.equals("198200004")){
            siteId =  siteName.substring(0,siteName.indexOf("_")).trim();
        }
        else {
            String version_1_from_field_26_length_7 = siteName.substring(0, 7).trim();

            char fourthChar = siteName.charAt(3);

            if (fourthChar == 'P' || fourthChar == 'C' || fourthChar == 'U') {
                // Set siteId for 'P', 'C', or 'U' in the 4th position
                siteId = version_1_from_field_26_length_7;
            } else if (fourthChar == 'M') {
                // Check for 'M' in the 4th position
                if (siteName.charAt(4) == 'U') {
                    // If 5th char is 'U', set siteId using first 8 characters
                    siteId = siteName.substring(0, 8).trim();
                } else if (siteName.charAt(6) == '(') {
                    // If 7th char is '(', set siteId using first 6 characters
                    siteId = siteName.substring(0, 6).trim();
                } else {
                    // Default case for 'M' in 4th position, use first 7 characters
                    siteId = version_1_from_field_26_length_7;
                }
            } else if (fourthChar == 'L') {
                // Set siteId for 'L' in the 4th position
                siteId = version_1_from_field_26_length_7;
            } else if (siteName.contains("_UL_")) {
                // If "_UL_" is found, set siteId using first 9 characters
                siteId = siteName.substring(0, 9).trim();
            } else {
                // Default case, set siteId using first 6 characters
                siteId = siteName.substring(0, 6).trim();
            }
        }


        zteTrapBody.setSiteName(siteName);
        zteTrapBody.setSiteId(siteId);
        zteTrapBody.setAlarmNodeBId(nodeBId);
        zteTrapBody.setAlarmRncId(rncId);

        return zteTrapBody;

    }

    // Helper method to get RNC ID for specific site names
    private String getRncIdForSite(String siteName) {
        switch (siteName) {
            case "GDZRZ01(7)":
                return "7";
            case "HRTZR01-RNC01(3)":
                return "3";
            case "KDRZR01-RNC01(4)":
                return "4";
            default:
                return "";
        }
    }

    public String setUpServiceType(String siteId, String alarmCodeString) {

        String localServiceType;

        // Assume alarmCodeString is already defined as a String containing the alarm code
        Long alarmCode = Long.parseLong(alarmCodeString);


       // Check conditions
        if (alarmCode == 200083023 || alarmCode == 200083022 || alarmCode == 199083023 || alarmCode == 199083022 || alarmCode == 198083023 || alarmCode == 198083022) {
            localServiceType = "3G";
        } else if (alarmCode == 198094422 || alarmCode == 198094420 || alarmCode == 198094419 || alarmCode == 198099803 || alarmCode == 198200011 || alarmCode == 198200001 ||
                alarmCode == 1014 || alarmCode == 198094466 || alarmCode == 198200004) {
            localServiceType = "4G";
        }
        else {
            // Handle other cases if needed
            localServiceType = "2G";
        }




        return localServiceType;

    }


    private void appendData(ZteTrapBody zte) {
        try {
            Files.write(Paths.get(FILE_PATH), (zte.toString() + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void appendData(PDU pdu,String file) {
        try {
            Files.write(Paths.get(file), (pdu.toString() + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void appendData(ZteTrapBody pdu,String file) {
        try {
            Files.write(Paths.get(file), (pdu.toString() + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public String setUpDisplaySiteId(String tempSiteId, String temServiceType,String alarmRncId, String alarmNodeBId) {

        String localDisplaySiteId;
//        System.out.println("Display-2: "+temServiceType);

        if(alarmRncId != null && alarmNodeBId != null && temServiceType.equalsIgnoreCase("3G")){
            localDisplaySiteId = null;
        }else{
            if(temServiceType.equalsIgnoreCase("3G")){
                System.out.println("Display-3: "+tempSiteId.substring(0,4).concat("U").concat(tempSiteId.substring(4)));

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
