package com.traps.RoshanNOCTraps.traps.hw;


import com.mycompany.app.sharedClasses.BssHwTrapBody;

import com.traps.RoshanNOCTraps.db.KafkaOperation;

import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;

@Component
public class ProcessHwPdu {



    public void processPdu(CommandResponderEvent crEvent) throws SQLException {

        PDU pdu = crEvent.getPDU();
        processHwPDU(pdu);

    }


    private void processHwPDU(PDU pdu) throws SQLException {


        try {
            if (pdu.getType() == PDU.TRAP) {

                Long intendedAlarmHw = getVariableAsLong(pdu,HwOidConstants.HW_INTENDED_ALARM);
                if (HwOidConstants.alarmIdList.contains(intendedAlarmHw)) {
                    BssHwTrapBody hwTrapBody = createHuaweiTrapBody(pdu,intendedAlarmHw);
                    KafkaOperation.sendHwTrap(hwTrapBody);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }



    private BssHwTrapBody createHuaweiTrapBody(PDU pdu, Long intendedAlarmHw) {
        BssHwTrapBody hwTrapBody = new BssHwTrapBody();

        hwTrapBody.setTrapId(getVariableAsString(pdu, HwOidConstants.HW_TRAP_ID));
        hwTrapBody.setAlarmClearedTime(getVariableAsString(pdu, HwOidConstants.HW_ALARM_CLEARED_TIME));
        hwTrapBody.setSiteName(getVariableAsString(pdu, HwOidConstants.HW_SITE_NAME));
        hwTrapBody.setAlarmArrivalTime(getVariableAsString(pdu, HwOidConstants.HW_EVENT_TIME));

        String clearOrNot = getVariableAsString(pdu, HwOidConstants.HW_CLEAR_OR_NOT);

        hwTrapBody.setAlarmCode(intendedAlarmHw.toString());

        hwTrapBody.setAlarmName(getVariableAsString(pdu, HwOidConstants.HW_ALARM_NAME));
        hwTrapBody.setAlarmEventType(getVariableAsLong(pdu, HwOidConstants.HW_ALARM_EVENT_TYPE));
        hwTrapBody.setAlarmNetType(getVariableAsString(pdu, HwOidConstants.HW_ALARM_NET_TYPE));
        hwTrapBody.setAlarmSeverity(getVariableAsLong(pdu, HwOidConstants.HW_ALARM_SEVERITY));

        String objectInstanceNameHw = getVariableAsString(pdu, HwOidConstants.HW_OBJECT_INSTANCE_NAME);
        hwTrapBody = extractSiteInfoHW(objectInstanceNameHw.trim(), hwTrapBody);


        hwTrapBody.setSiteId(setUpSiteId(hwTrapBody.getSiteId()));
        hwTrapBody.setAlarmServiceType(setUpServiceType(hwTrapBody.getSiteId(), hwTrapBody.getAlarmCode()));
        hwTrapBody.setDisplaySiteId(setUpDisplaySiteId(hwTrapBody.getSiteId(), hwTrapBody.getAlarmServiceType()));

        if (isNewHuaweiAlarm(clearOrNot)) {
            hwTrapBody.setNewOrClear(1L);
        } else {
            hwTrapBody.setNewOrClear(2L);
        }

        logTrap(hwTrapBody);
        return hwTrapBody;
    }


    /// GOTTA SHARE WITH ANOTHER FILE

    public Long getVariableAsLong(PDU pdu, OID oid) {
        try {
            String value = getVariableAsString(pdu, oid);
            return value.isEmpty() ? null : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    public String getVariableAsString(PDU pdu, OID oid) {
        Variable variable = pdu.getVariable(oid);
        return variable != null ? variable.toString() : "";
    }

    public void appendData(PDU pdu,String file) {
        try {
            Files.write(Paths.get(file), (pdu.toString() + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }










    public void logTrap(BssHwTrapBody trap) {
        System.out.println("\nHW Trap Processed:");
        System.out.println("ID: " + trap.getTrapId());
        System.out.println("Alarm Code: " + trap.getAlarmCode());
        System.out.println("Site: " + trap.getSiteName());
        System.out.println("Type: " + (trap.getNewOrClear() == 1 ? "NEW" : "CLEARED"));
        System.out.println("*******************************************");
    }



    //////////////////////////////////////


//    private void processNewHuaweiAlarm(PDU pdu, BssHwTrapBody hwTrapBody, Long alarmCode, String eventTime) {
//
//        hwTrapBody.setAlarmArrivalTime(eventTime);
//        hwTrapBody.setAlarmCode(alarmCode.toString());
//
//        hwTrapBody.setAlarmName(getVariableAsString(pdu, HwOidConstants.HW_ALARM_NAME));
//        hwTrapBody.setAlarmEventType(getVariableAsLong(pdu, HwOidConstants.HW_ALARM_EVENT_TYPE));
//        hwTrapBody.setAlarmNetType(getVariableAsString(pdu, HwOidConstants.HW_ALARM_NET_TYPE));
//        hwTrapBody.setAlarmSeverity(getVariableAsLong(pdu, HwOidConstants.HW_ALARM_SEVERITY));
//
//        String objectInstanceNameHw = getVariableAsString(pdu, HwOidConstants.HW_OBJECT_INSTANCE_NAME);
//        hwTrapBody = extractSiteInfoHW(objectInstanceNameHw.trim(), hwTrapBody);
//
//
//        hwTrapBody.setSiteId(setUpSiteId(hwTrapBody.getSiteId()));
//        hwTrapBody.setAlarmServiceType(setUpServiceType(hwTrapBody.getSiteId(), hwTrapBody.getAlarmCode()));
//        hwTrapBody.setDisplaySiteId(setUpDisplaySiteId(hwTrapBody.getSiteId(), hwTrapBody.getAlarmServiceType()));
//
//        hwTrapBody.setNewOrClear(1L);
//    }


    private boolean isNewHuaweiAlarm(String clearOrNot) {
        return "2".equals(clearOrNot);
    }

    private boolean isClearedHuaweiAlarm(String clearOrNot) {
        return !"2".equals(clearOrNot);
    }

//    private void processClearedHuaweiAlarm(BssHwTrapBody hwTrapBody, Long alarmCode, String eventTime) {
//        hwTrapBody.setAlarmCode(alarmCode.toString());
//        hwTrapBody.setAlarmClearedTime(eventTime);
//        hwTrapBody.setAlarmArrivalTime(eventTime);
//
//        hwTrapBody.setAlarmName(getVariableAsString(pdu, HwOidConstants.HW_ALARM_NAME));
//        hwTrapBody.setAlarmEventType(getVariableAsLong(pdu, HwOidConstants.HW_ALARM_EVENT_TYPE));
//        hwTrapBody.setAlarmNetType(getVariableAsString(pdu, HwOidConstants.HW_ALARM_NET_TYPE));
//        hwTrapBody.setAlarmSeverity(getVariableAsLong(pdu, HwOidConstants.HW_ALARM_SEVERITY));
//
//        String objectInstanceNameHw = getVariableAsString(pdu, HwOidConstants.HW_OBJECT_INSTANCE_NAME);
//        hwTrapBody = extractSiteInfoHW(objectInstanceNameHw.trim(), hwTrapBody);
//
//
//        hwTrapBody.setSiteId(setUpSiteId(hwTrapBody.getSiteId()));
//        hwTrapBody.setAlarmServiceType(setUpServiceType(hwTrapBody.getSiteId(), hwTrapBody.getAlarmCode()));
//        hwTrapBody.setDisplaySiteId(setUpDisplaySiteId(hwTrapBody.getSiteId(), hwTrapBody.getAlarmServiceType()));
//
//
//        hwTrapBody.setNewOrClear(2L);
//    }




//    private void filterHuaweiTrap(PDU pdu,Long intendedAlarmHuawei) throws SQLException {
//
//
//
//        BssHwTrapBody hwTrapBody = new BssHwTrapBody();
//        hwTrapBody.setTrapId(pdu.getVariable(new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.1.0")).toString());
//        hwTrapBody.setAlarmClearedTime(pdu.getVariable(new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.15.0")).toString());
//     hwTrapBody.setAlarmArrivalTime(pdu.getVariable(new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.3.0")).toString());
//
//        String clearOrNot = pdu.getVariable(new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.12.0")).toString();
//
//
//        hwTrapBody.setSiteName(pdu.getVariable(new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.4.0")).toString());
//
//
//        String objectInstanceName_hw = pdu.getVariable(new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.27.0")).toString();
//
//        //event time section
//        hwTrapBody.setAlarmArrivalTime(pdu.getVariable(new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.3.0")).toString());
//
//        //alarm identification
//        hwTrapBody.setAlarmCode(intendedAlarmHuawei.toString());
//        hwTrapBody.setAlarmName(pdu.getVariable(new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.28.0")).toString());
//        hwTrapBody.setAlarmEventType(pdu.getVariable(new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.10.0")).toLong());
//        hwTrapBody.setAlarmNetType(pdu.getVariable(new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.6.0")).toString());
//
//        hwTrapBody.setAlarmSeverity(pdu.getVariable(new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.11.0")).toLong());
//
//
//        //set up site info
//        hwTrapBody = extractSiteInfoHW(objectInstanceName_hw.trim(),hwTrapBody);
//
//
//
//        //setup site id
//        hwTrapBody.setSiteId(setUpSiteId(hwTrapBody.getSiteId()));
//
//        //setup service type
//        hwTrapBody.setAlarmServiceType(setUpServiceType(hwTrapBody.getSiteId(),hwTrapBody.getAlarmCode()));
//
//        //setup display site id
//        hwTrapBody.setDisplaySiteId(setUpDisplaySiteId(hwTrapBody.getSiteId(),hwTrapBody.getAlarmServiceType()));
//
//
//
//        if (Long.parseLong(clearOrNot) == 2) {
//
//
//            hwTrapBody.setNewOrClear(1L);
////            produce to kafka
//            KafkaOperation.sendHwTrap(hwTrapBody);
//
//
//
//
//
////            DbOperation.addHwTrap(hwTrapBody);
////            this.saveOrUpdateDatabaseHW("insert",pdu);
//        }else{
//
//            hwTrapBody.setNewOrClear(2L);
////            hwTrapBody.setId(DbOperation.generateUniqueId());
////            produce to kafka
//            KafkaOperation.sendHwTrap(hwTrapBody);
//
//
//
//
////            System.out.println("HW trap UPDATE: "+hwTrapBody);
////            DbOperation.updateHwTrap(hwTrapBody.getTrapId(), hwTrapBody);
////            this.saveOrUpdateDatabaseHW("update",pdu);
//        }
//
//
//        System.out.println("HW trap UPDATE: "+hwTrapBody);
////
//
////
//        System.out.println("*******************************************");
//
//
//    }
//




    private BssHwTrapBody extractSiteInfoHW(String info, BssHwTrapBody hwTrapBody) {
        String siteId = null;
        String siteName = hwTrapBody.getSiteName().trim();
        String alarmCodeHW = hwTrapBody.getAlarmCode();

        // Handle alarm code "22214"
        if (alarmCodeHW.equals("22214")) {
            siteName = extractSiteNameFromInfo(info, 4);
            siteId = extractSiteIdFromSiteName(siteName);
        } else {
            String untilUnderScoreOnly = null;
            String fromDashUntilUnderscore = null;
            String ifHas_UL_From0Until_UL_ = null;

            int dashIndex = siteName.indexOf("-");
            int underscoreIndex = siteName.indexOf("_");


            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            fromDashUntilUnderscore = getSiteIdFromDashToUnderScore(siteName, fromDashUntilUnderscore, dashIndex, underscoreIndex);
            ifHas_UL_From0Until_UL_ = getUntil_UL_PartOfAString(siteName);


            switch (alarmCodeHW) {
                case "25621":
                case "25622":

                    siteId = resolveSiteIdVersion1(siteName, untilUnderScoreOnly, fromDashUntilUnderscore, ifHas_UL_From0Until_UL_);
                    break;
                case "29201":
                case "65059":
                case "65068":
                case "65069":
                case "65080":
                case "65081":
                case "65084":
                case "65090":
                    siteId = resolveSiteIdVersion2(siteName, untilUnderScoreOnly, fromDashUntilUnderscore, ifHas_UL_From0Until_UL_);

                    if(siteId.equalsIgnoreCase("E-CENA") || siteId.equalsIgnoreCase("e-Khis")){
                        System.out.println("Found: "+hwTrapBody.toString());
                    }

                    break;


//                case "65033":
//                case "65070":
//                case "21807":
//                case "65071":
//                case "5700":
//                case "65501":
//                    siteId = fromDashUntilUnderscore;
//                    break;

                case "65067":
                    siteId = resolveSiteIdVersion3(siteName, untilUnderScoreOnly, fromDashUntilUnderscore, ifHas_UL_From0Until_UL_);
                    break;


                case "21825":

                case "65502":
                case "65033":
                case "65070":
                case "21807":
                case "65071":
                case "5700":
                case "65501":

                    siteId = siteName.contains("-") ? fromDashUntilUnderscore : untilUnderScoreOnly;
                    break;

                default:
                    if (siteId == null || siteId.isEmpty()) {
                        siteId = "RANDOM";
                    }
                    break;
            }
        }

        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);
        return hwTrapBody;
    }

    private static String getSiteIdFromDashToUnderScore(String siteName, String fromDashUntilUnderscore, int dashIndex, int underscoreIndex) {



        if (dashIndex != -1 && underscoreIndex != -1) {
            // Case 1: Extract the substring between the dash and underscore
            if (dashIndex < underscoreIndex) {
                // Case 1: First dash before underscore
                fromDashUntilUnderscore = siteName.substring(dashIndex + 1, underscoreIndex).trim();
                System.out.println("fromDashUntilUnderscore: " + fromDashUntilUnderscore);
            } else {
                // Case 2: If underscore appears before dash, adjust extraction
                // Extracting from dash to next underscore (we assume the next underscore is after the first dash)
                int nextUnderscoreIndex = siteName.indexOf("_", dashIndex);
                if (nextUnderscoreIndex != -1) {
                    fromDashUntilUnderscore = siteName.substring(dashIndex + 1, nextUnderscoreIndex).trim();
                    System.out.println("fromDashUntilUnderscore: " + fromDashUntilUnderscore);
                }
            }
        }
        return fromDashUntilUnderscore;
    }

    String getUntilUnderscorePartOfAString(String siteName, int underscoreIndex){

        if (underscoreIndex != -1) {
            return siteName.substring(0, siteName.indexOf("_")).trim();

        }
        return null;
    }
    String getUntil_UL_PartOfAString(String siteName){

        if (siteName.contains("_UL_")) {
            return siteName.substring(0, siteName.indexOf("_UL_") + 3).trim();
        }
        return null;
    }



    // Helper method to extract site name from info
    private String extractSiteNameFromInfo(String info, int index) {
        String[] arr = info.split(",");

        // Ensure the index is within bounds; default to index 2 if out of range
        if (index < 0 || index >= arr.length) {
            index = 2; // Default to 2 if the given index is out of bounds
        }

        if (index >= arr.length) { // If index 2 is still out of bounds, return an empty string or handle gracefully
            return null;
        }


        String term = arr[index].trim();

        // Ensure the term contains "=" before attempting substring extraction
        int equalsIndex = term.indexOf("=");
        if (equalsIndex == -1 || equalsIndex == term.length() - 1) {
            return ""; // Return empty string if "=" is missing or there's no value after "="
        }

        return term.substring(equalsIndex + 1).trim();
    }

    // Helper method to extract site ID from site name
    private String extractSiteIdFromSiteName(String siteName) {
        return siteName.substring(0, siteName.indexOf("_")).trim();
    }


    private String resolveSiteIdVersion2(String siteName, String untilUnderScoreOnly, String fromDashUntilUnderscore, String ifHas_UL_From0Until_UL_) {

        if (siteName.contains("_UL_")) {
            return ifHas_UL_From0Until_UL_;
        } else if (siteName.contains("-")) {
            return fromDashUntilUnderscore;
        } else {
            return untilUnderScoreOnly;
        }
    }

    //IMPORTNAT TEST
    private String resolveSiteIdVersion3(String siteName, String untilUnderScoreOnly, String fromDashUntilUnderscore, String ifHas_UL_From0Until_UL_) {



        int dashIndex = siteName.indexOf("-");
        int underscoreIndex = siteName.indexOf("_");

        if (siteName.contains("_UL_")) {
            return ifHas_UL_From0Until_UL_;
        } else {

            if (dashIndex != -1 && underscoreIndex != -1) {
                // Case 1: Extract the substring between the dash and underscore
                if (dashIndex < underscoreIndex) {

                    if(fromDashUntilUnderscore.contains("-")){
                        return untilUnderScoreOnly;
                    }
                   return fromDashUntilUnderscore;
                }
            }
            return untilUnderScoreOnly;
        }
    }


    private String resolveSiteIdVersion4(String siteName, String untilUnderScoreOnly, String fromDashUntilUnderscore, String ifHas_UL_From0Until_UL_) {




//        KBL309_UL_QZ //ok
//        JLDH02-JLD027_25_WIYALA_P3
//        KBL039_MBSC-KBL266_SHAHRAK_E_TELAHE_PT_P3
        //KBLU168_SYEED_OMAR_Market_Pul-e-Khishti_CPX_P2

//        KBLU342_QALAE_AHMAD_KHAN_BAGRAMI_SALAAM(KAB302)_P3
        //        KBL309_UL



        int dashIndex = siteName.indexOf("-");
        int underscoreIndex = siteName.indexOf("_");

        if (siteName.contains("_UL_")) {
            return ifHas_UL_From0Until_UL_;
        } else {

            if (dashIndex != -1 && underscoreIndex != -1) {
                // Case 1: Extract the substring between the dash and underscore
                if (dashIndex < underscoreIndex) {
                    return fromDashUntilUnderscore;
                }
            }
            return untilUnderScoreOnly;
        }
    }

    // Helper method to resolve site ID based on versions
    private String resolveSiteIdVersion1(String siteName, String untilUnderScoreOnly, String fromDashUntilUnderscore, String ifHas_UL_From0Until_UL_) {



        String localSiteId = fromDashUntilUnderscore;
        if(siteName.contains("_UL_")){
            return ifHas_UL_From0Until_UL_;
        }else{
            if(localSiteId.contains("-") ||  localSiteId.length() < 6){
                return untilUnderScoreOnly;
            }else{
                return fromDashUntilUnderscore;
            }
        }


    }





    public String setUpServiceType(String siteId, String alarmCodeString) {



        String localServiceType;

        // Assume alarmCodeString is already defined as a String containing the alarm code
        Long alarmCode = Long.parseLong(alarmCodeString);

        if(alarmCode == 22214L){
            localServiceType = "3G";
        }

        // Check conditions
        // Check if alarmCode matches any of the predefined values
        if (alarmCode == 22214 || alarmCode == 22202 || alarmCode == 65081 ||
                alarmCode == 65080 || alarmCode == 65070 || alarmCode == 65069 ||
                alarmCode == 65068 || alarmCode == 65067 || alarmCode == 25622 ||
                alarmCode == 25621 || alarmCode == 65334) {
            localServiceType = "3G";
        } else if (alarmCode == 29201 || alarmCode == 21825 || alarmCode == 18606 || alarmCode == 65090) {
            localServiceType = "4G";
        } else if(alarmCode == 65069L){

            if(siteId.endsWith("_UL")){
                localServiceType = "4G";
            }else{
                localServiceType = "3G";
            }
        }
        else {
            // Handle other cases if needed
            localServiceType = "2G";
        }


//        String localServiceType;
//        Long alarmCode = Long.parseLong(alarmCodeString);
//
//        List<String> alarmCodes3G = Arrays.asList(
//                "22202", "65067", "65080", "198083022", "199083022",
//                "65068", "65381", "198083023", "199083023", "200083022", "200083023"
//        );
//
//        List<String> alarmCodes4G = Arrays.asList(
//                "65081", "29201", "198094419", "198094461",
//                "65084", "198092295", "198094422"
//        );
//
//        if(alarmCode == 22214L){
//            localServiceType = "3G";
//        }
//        else if (alarmCodes3G.contains(alarmCode)) {
//            localServiceType = "3G";
//        } else if (alarmCodes4G.contains(alarmCode)) {
//            localServiceType = "4G";
//        }
//        else if(alarmCode == 65069L){
//
//            if(siteId.endsWith("_UL")){
//                localServiceType = "4G";
//            }else{
//                localServiceType = "3G";
//            }
//        }
//        else{
//            localServiceType = "2G";
//        }

        return localServiceType;

    }

    public String setUpDisplaySiteId(String tempSiteId, String temServiceType) {

        String localDisplaySiteId;

        if(temServiceType.equalsIgnoreCase("3G")){
            if(tempSiteId.charAt(3) == 'M'){ //kbl
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


    public void appendData(PDU pdu, String folder, String fileName) {
        try {
            // Ensure the directory exists
            Path dirPath = Paths.get(folder);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // Construct full file path
            Path filePath = dirPath.resolve(fileName);

            // Write data to the file
            Files.write(filePath, (pdu.toString() + System.lineSeparator()).getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    private void appendData(PDU pdu, String file) {
//        try {
//            Files.write(Paths.get(file), (pdu.toString() + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void appendData(BssHwTrapBody pdu, String file) {
//        try {
//            Files.write(Paths.get(file), (pdu.toString() + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    private void appendData(BssHwTrapBody hwTrapBody) {
//        try {
//            Files.write(Paths.get(FILE_PATH), (hwTrapBody.toString() + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void appendData(PDU pdu) {
//        try {
//            Files.write(Paths.get(FILE_PATH), (pdu.toString() + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

}
