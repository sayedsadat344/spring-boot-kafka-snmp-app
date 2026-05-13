package com.traps.RoshanNOCTraps.traps.hw;


import com.mycompany.app.sharedClasses.BssHwTrapBody;

import com.traps.RoshanNOCTraps.db.KafkaOperation;
import com.traps.RoshanNOCTraps.traps.ServiceTypes;
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

//                    appendData(pdu,"BSSHW",intendedAlarmHw.toString());
                    BssHwTrapBody hwTrapBody = createHuaweiTrapBody(pdu,intendedAlarmHw);
//                    appendData(hwTrapBody,"BSSHW",intendedAlarmHw.toString());

                    KafkaOperation.sendHwTrap(hwTrapBody);

                    logTrap(hwTrapBody);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void appendData(BssHwTrapBody pdu, String folderName, String fileName) {
        try {
            Path folderPath = Paths.get(folderName);
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath); // Ensure the folder exists
            }

            Path filePath = folderPath.resolve(fileName); // Construct full file path

            Files.write(
                    filePath,
                    (pdu.toString() + System.lineSeparator()).getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

        } catch (IOException e) {
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



        hwTrapBody.setAlarmServiceType(setUpServiceType(hwTrapBody.getSiteId(), hwTrapBody.getAlarmCode(),hwTrapBody.getSiteName()));
        hwTrapBody.setDisplaySiteId(setUpDisplaySiteId(hwTrapBody.getSiteId(), hwTrapBody.getAlarmServiceType(),intendedAlarmHw));

        if (isNewHuaweiAlarm(clearOrNot)) {
            hwTrapBody.setNewOrClear(1L);
        } else {
            hwTrapBody.setNewOrClear(2L);
        }


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






    private boolean isNewHuaweiAlarm(String clearOrNot) {
        return "2".equals(clearOrNot);
    }









    private BssHwTrapBody extractSiteInfoHW(String info, BssHwTrapBody hwTrapBody) {

//        info = 27, sitename = 4

        String siteId = null;
        String siteName = hwTrapBody.getSiteName().trim();
        String alarmCodeHW = hwTrapBody.getAlarmCode();

        // Handle alarm code "22214"
        //no issue
//        || alarmCodeHW.equals("21801")
        if (alarmCodeHW.equals("22214") ) {
            siteName = extractSiteNameFromInfo(info, 4,alarmCodeHW);
            siteId = extractSiteIdFromSiteName(siteName,alarmCodeHW);
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

                    siteId = extractLastCode25621(siteName);

                    break;
                case "25622":

                    siteId = resolveSiteIdVersion1(siteName, untilUnderScoreOnly, fromDashUntilUnderscore, ifHas_UL_From0Until_UL_,alarmCodeHW);
                    break;
                case "29201":
//                case "65059":
//                case "65068":
                case "65069":
                case "65080":
                case "21805":
                case "65084":
                case "65090":
                    siteId = resolveSiteIdVersion2(siteName, untilUnderScoreOnly, fromDashUntilUnderscore, ifHas_UL_From0Until_UL_,alarmCodeHW);

                    if(siteId.equalsIgnoreCase("E-CENA") || siteId.equalsIgnoreCase("e-Khis")){
                        System.out.println("Found: "+hwTrapBody.toString());
                    }

                    break;



                case "65067":
                    siteId = resolveSiteIdVersion3(siteName, untilUnderScoreOnly, fromDashUntilUnderscore, ifHas_UL_From0Until_UL_);
                    break;

                case "65050":

                    siteId = fromDashUntilUnderscore;
                    break;

                case "65033":

                    siteId = siteName.contains("-") ? fromDashUntilUnderscore : untilUnderScoreOnly;
                    if(siteId.contains("-")){
                        siteId = untilUnderScoreOnly;
                    }
                    break;

                case "21825":

//                case "65502":

                case "65070":
                case "21807":
                case "65071":
                case "5700":
                case "65501":
//                case "65034":

                    siteId = siteName.contains("-") ? fromDashUntilUnderscore : untilUnderScoreOnly;
                    break;

//                case "25888":
                case "29213":
//                case "65091":
                case "26108":
                        siteId = untilUnderScoreOnly;
                        break;



                    //based on partial data
//                case "65412":
//                    siteId =  fromDashUntilUnderscore ;
//
//                    break;


//                case "21541":
//                    siteName = extractSiteNameFromInfo(info,7,alarmCodeHW);
//
//                    siteId = extractSiteIdFromSiteName(siteName, alarmCodeHW);
//
//
//                    break;
//                case "22202":
//                    siteName = extractSiteNameFromInfo(info,3);
//
//                    siteId = extractSiteIdFromSiteName(siteName, alarmCodeHW);
//
//
//                    break;

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
//                System.out.println("fromDashUntilUnderscore: " + fromDashUntilUnderscore);
            } else {
                // Case 2: If underscore appears before dash, adjust extraction
                // Extracting from dash to next underscore (we assume the next underscore is after the first dash)
                int nextUnderscoreIndex = siteName.indexOf("_", dashIndex);
                if (nextUnderscoreIndex != -1) {
                    fromDashUntilUnderscore = siteName.substring(dashIndex + 1, nextUnderscoreIndex).trim();
//                    System.out.println("fromDashUntilUnderscore: " + fromDashUntilUnderscore);
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
    private String extractSiteNameFromInfo(String info,int offset) {
        String[] arr = info.split(",");
//        int index = 7;

        int index = arr.length - offset;

        if (index < 0 || index >= arr.length) {
            index = arr.length - offset;
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

    // Helper method to extract site name from info
    private String extractSiteNameFromInfo(String info, int index , String code) {

//        System.out.println("info : "+info);
        String[] arr = info.split(",");

        if(code.equals("21541")){
            index = arr.length - 2;
        }


        // Ensure the index is within bounds; default to index 2 if out of range
        if (index < 0 || index >= arr.length) {

            if(code.equals("22214")){
                // Default to 2  (22214 alarm)
                index = 2;
            }else{
                index = arr.length - 2;
            }

        }

        // If index 2 is still out of bounds, return an empty string or handle gracefully
        if (index >= arr.length) {
            return null;
        }


        String term = arr[index].trim();

//        System.out.println("Index: "+index);
//        System.out.println("Term:  "+term);


        // Ensure the term contains "=" before attempting substring extraction
        int equalsIndex = term.indexOf("=");
        if (equalsIndex == -1 || equalsIndex == term.length() - 1) {
            return "";
        }

        return term.substring(equalsIndex + 1).trim();
    }

    // Helper method to extract site ID from site name
    private String extractSiteIdFromSiteName(String siteName, String alarmCodeHW) {

//        System.out.println("Site name: "+siteName + " -------------------    Code: "+alarmCodeHW);

        if(siteName.contains("_")){
            return siteName.substring(0, siteName.indexOf("_")).trim();
        }

        return siteName;
    }




    private String resolveSiteIdVersion2(String siteName, String untilUnderScoreOnly, String fromDashUntilUnderscore, String ifHas_UL_From0Until_UL_, String alarmCodeHW) {


        if (siteName.contains("_UL_")) {
            return ifHas_UL_From0Until_UL_;
        } else if(siteName.contains("_UL")){
            return untilUnderScoreOnly + "_UL";
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





    public String extractLastCode25621(String input) {
        if (input == null || input.isEmpty()) return null;

        String searchPart = input;

        // If there's a hyphen, search after the first hyphen
        int dashIndex = input.indexOf('-');
        if (dashIndex != -1) {
            searchPart = input.substring(dashIndex + 1);
        }

        // Pattern: 2–5 letters followed by exactly 3 digits
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([A-Za-z]{2,5}\\d{3})");
        java.util.regex.Matcher matcher = pattern.matcher(searchPart);

        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }

        return null;
    }

    // Helper method to resolve site ID based on versions
    private String resolveSiteIdVersion1(String siteName, String untilUnderScoreOnly, String fromDashUntilUnderscore, String ifHas_UL_From0Until_UL_, String alarmCodeHW) {



        String localSiteId = fromDashUntilUnderscore;
        if(siteName.contains("_UL_")){
            return ifHas_UL_From0Until_UL_;
        }else{
            if(localSiteId != null && (localSiteId.contains("-") ||  localSiteId.length() < 6)){
                return untilUnderScoreOnly;
            }else{
                return fromDashUntilUnderscore;
            }
        }


    }





    public String setUpServiceType(String siteId, String alarmCodeString, String siteName) {



        long alarmCode;
        try {
            alarmCode = Long.parseLong(alarmCodeString);
        } catch (NumberFormatException e) {
            return "UNKNOWN"; // invalid input
        }

        if (ServiceTypes.CODES_2G.contains(alarmCode)) {
            return "2G";
        } else if (ServiceTypes.CODES_3G.contains(alarmCode)) {
            return "3G";
        } else if (ServiceTypes.CODES_4G.contains(alarmCode)) {
            return "4G";
        } else if (ServiceTypes.CODES_2G_3G.contains(alarmCode) ||
                ServiceTypes.CODES_3G_4G.contains(alarmCode) ||
                ServiceTypes.CODES_2G_3G_4G.contains(alarmCode)) {
            return ServiceTypes.checkMultiServiceTypes(siteName, siteId, alarmCode);
        } else {
            return "UNKNOWN"; // fallback for unmapped codes
        }


//        String localServiceType;
//
//        if(alarmCode == 22214L){
//            localServiceType = "3G";
//        }
//
//        // Check conditions
//        // Check if alarmCode matches any of the predefined values
//        if (alarmCode == 22214 || alarmCode == 22202 || alarmCode == 65081 ||
//                alarmCode == 65080 || alarmCode == 65070 || alarmCode == 65069 ||
//                alarmCode == 65068 || alarmCode == 65067 || alarmCode == 25622 ||
//                alarmCode == 25621 || alarmCode == 65334) {
//            localServiceType = "3G";
//        } else if (alarmCode == 29201 || alarmCode == 21825 || alarmCode == 18606 || alarmCode == 65090) {
//            localServiceType = "4G";
//        } else if(alarmCode == 65069L){
//
//            if(siteId.endsWith("_UL")){
//                localServiceType = "4G";
//            }else{
//                localServiceType = "3G";
//            }
//        }
//        else {
//            // Handle other cases if needed
//            localServiceType = "2G";
//        }
//
//
//
//
//        return localServiceType;

    }

    public String setUpDisplaySiteId(String tempSiteId, String temServiceType, Long intendedAlarmHw) {

        System.out.println("*******************************");
        System.out.println("Code: "+intendedAlarmHw);
        System.out.println("Tempsite id: "+tempSiteId);
        System.out.println("Temp Service Type: "+temServiceType);
        System.out.println("*******************************");

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

//        System.out.println("*************************************");
//
//
//        System.out.println("Site id: "+siteId);



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

//        System.out.println("Modified Id: "+result);
//
//        System.out.println("****************************************");

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
