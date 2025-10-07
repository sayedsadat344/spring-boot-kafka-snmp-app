package com.traps.RoshanNOCTraps.traps.zte;


import com.mycompany.app.sharedClasses.BssZteTrapBody;

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


import static com.traps.RoshanNOCTraps.db.DbOperation.generateUniqueId;

@Component
public class ProcessZtePdu {


    public void processPdu(CommandResponderEvent crEvent) throws SQLException {

        PDU pdu = crEvent.getPDU();
        processZTEPDU(pdu);

    }

    private void processZTEPDU(PDU pdu) throws SQLException {
        try {
            if(pdu != null && pdu.getType() == PDU.TRAP){
                Long intendedAlarmZte = getVariableAsLong(pdu, ZteOidConstants.ALARM_CODE);
                if(ZteOidConstants.alarmValues.contains(intendedAlarmZte)){
                    appendData(pdu, "BSSZTE", intendedAlarmZte.toString());


                    BssZteTrapBody trapBody = createBssZteTrapBody(pdu);

                    if(trapBody != null){
                        appendData(trapBody, "BSSZTE", intendedAlarmZte.toString());
                        KafkaOperation.sendZteTrap(trapBody);
                        logTrap(trapBody);
                    }


                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    ////GOTTA SHARE IN ANOTHER CALLSSS


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

    public void appendData(PDU pdu, String folder, String fileName) {
        try {
            // Ensure the directory exists
            Path dirPath = Paths.get(folder);
            if(!Files.exists(dirPath)){
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


    private void appendData(BssZteTrapBody zte) {
        try {
            Files.write(Paths.get(ZteOidConstants.FILE_PATH), (zte.toString() + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void appendData(BssZteTrapBody pdu, String file) {
        try {
            Files.write(Paths.get(file), (pdu.toString() + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void appendData(BssZteTrapBody pdu, String folderName, String fileName) {
        try {
            Path folderPath = Paths.get(folderName);
            if(!Files.exists(folderPath)){
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


    public void logTrap(BssZteTrapBody trap) {
        System.out.println("\nZTE Trap Processed:");
        System.out.println("ID: " + trap.getTrapId());
        System.out.println("Alarm Code: " + trap.getAlarmCode());
        System.out.println("Site: " + trap.getSiteName());
        System.out.println("Type: " + (trap.getNewOrClear() == 1 ? "NEW" : "CLEARED"));
        System.out.println("*******************************************");
    }


    private BssZteTrapBody createBssZteTrapBody(PDU pdu) {


        BssZteTrapBody zteTrapBody = new BssZteTrapBody();
        // Extract basic fields using constants
        zteTrapBody.setTrapId(getVariableAsString(pdu, ZteOidConstants.ALARM_TRAP_ID));
        zteTrapBody.setAlarmCode(getVariableAsString(pdu, ZteOidConstants.ALARM_CODE));
        String eventTime = getVariableAsString(pdu, ZteOidConstants.ALARM_EVENT_TIME);
        String alarmNewOrClear = getVariableAsString(pdu, ZteOidConstants.SNMP_TRAP_OID);

        // Process based on trap type
        if(isNewAlarm(alarmNewOrClear)){
            processNewAlarm(pdu, zteTrapBody, eventTime);
        } else if(isClearedAlarm(alarmNewOrClear)){
            processClearedAlarm(zteTrapBody, eventTime);
        }


        return zteTrapBody;

    }

    private boolean isNewAlarm(String trapOid) {
        return ZteOidConstants.ALARM_NEW_TRAP.equals(new OID(trapOid));
    }

    private boolean isClearedAlarm(String trapOid) {
        return ZteOidConstants.ALARM_CLEARED_TRAP.equals(new OID(trapOid)) || ZteOidConstants.ALARM_CLEARED_TRAP_2.equals(new OID(trapOid));
    }

    private void processNewAlarm(PDU pdu, BssZteTrapBody zteTrapBody, String eventTime) {
        zteTrapBody.setAlarmArrivalTime(eventTime);
        zteTrapBody.setAlarmName(getVariableAsString(pdu, ZteOidConstants.ALARM_NAME));
        zteTrapBody.setSiteName(getVariableAsString(pdu, ZteOidConstants.SITE_NAME));

        String localRNCId = getVariableAsString(pdu, ZteOidConstants.LOCAL_RNC_ID);
        String objectInstanceName = getVariableAsString(pdu, ZteOidConstants.OBJECT_INSTANCE_NAME);

        // Extract site info (keep your existing method)
        zteTrapBody = extractSiteInfoZTE(zteTrapBody, objectInstanceName, localRNCId);

        if(zteTrapBody == null){
            return;
        }

        // Setup site info (keep your existing methods)
        zteTrapBody.setSiteId(setUpSiteId(zteTrapBody.getSiteId()));
        zteTrapBody.setAlarmServiceType(setUpServiceType(
                zteTrapBody.getSiteId(),
                zteTrapBody.getAlarmCode(), zteTrapBody.getSiteName()
        ));
        zteTrapBody.setDisplaySiteId(setUpDisplaySiteId(
                zteTrapBody.getSiteId(),
                zteTrapBody.getAlarmServiceType(),
                zteTrapBody.getAlarmRncId(),
                zteTrapBody.getAlarmNodeBId()
        ));

        // Other details
        zteTrapBody.setAlarmEventType(getVariableAsLong(pdu, ZteOidConstants.ALARM_EVENT_TYPE));
        zteTrapBody.setNewOrClear(1L);
        zteTrapBody.setId(generateUniqueId());
    }

    private void processClearedAlarm(BssZteTrapBody zteTrapBody, String eventTime) {
        zteTrapBody.setAlarmClearedTime(eventTime);
        zteTrapBody.setNewOrClear(2L);
    }


    private BssZteTrapBody extractSiteInfoZTE(BssZteTrapBody zteTrapBody, String objectInstanceName_zte, String localRNCId) {

        //15 rnc and 8 obj  site name: 26
        String alarmCode = zteTrapBody.getAlarmCode();
        String nodeBId = null;
        String rncId = null;
        String siteId = null;
        String siteName = zteTrapBody.getSiteName();


        if(alarmCode.equals("199087337") || alarmCode.equals("198087337")){


            String arr[] = localRNCId.split(",");


            if(arr.length < 3){
                return null;
            }

            siteName = arr[2].trim();

            if(siteName.contains("_")){
                siteId = siteName.substring(0, siteName.indexOf("_")).trim();
            } else if(siteName.contains("(")){
                siteId = siteName.substring(0, siteName.indexOf("("));
            } else {
                siteId = siteName.length() > 7 ? siteName.substring(0, 7).trim() : siteName;
            }

            if(siteId.equalsIgnoreCase("GBtsEq")){
                System.out.println("Found: " + zteTrapBody.toString());

                //do something

            }

        }
//        else if(alarmCode.equals("199087342")){
//            String arr[] = localRNCId.split(",");
//            String term = arr[2].trim();
//            zteTrapBody.setSiteName(term);
//            siteId = term.substring(0, term.indexOf("_"));
//
//        }
//        || alarmCode.equals("198083022")
        else if(alarmCode.equals("199083023") || alarmCode.equals("198083023") ){
            String extractRncIdFromSiteName = siteName.substring(siteName.indexOf("(") + 1, siteName.length() - 1).trim();

            String arr[] = objectInstanceName_zte.split(";");

//            || alarmCode.equals("199083022")
            if(alarmCode.equals("199083023") ){
                nodeBId = arr[1].trim();
            } else {
                nodeBId = arr[0].trim();
            }

            nodeBId = nodeBId.substring(nodeBId.indexOf(":") + 1).trim();

            // Extract RNC ID from Site Name
            rncId = extractRncIdFromSiteName;

            // If RNC ID is empty, assign a default value based on the site name
            if(rncId.isEmpty()){
                rncId = getRncIdForSite(siteName);
            }


        }
//        || alarmCode.equals("198094419")
//        || alarmCode.equals("198092559")
        else if(alarmCode.equals("198092295") || alarmCode.equals("198092550")  || alarmCode.equals("198092562")
                || alarmCode.equals("198094422") || alarmCode.equals("1014") || alarmCode.equals("198094466") || alarmCode.equals("198200004") || alarmCode.equals("198092551") || alarmCode.equals("198092552")){

            if(siteName.contains("_")){
                siteId = siteName.substring(0, siteName.indexOf("_")).trim();
            } else if(siteName.contains("(") && siteName.contains(")")){
                siteId = siteName.substring(0, siteName.indexOf("(")).trim();
            } else if(siteName.contains("(")){
                siteId = siteName.substring(0, siteName.indexOf("("));
            } else {
                siteId = siteName.length() > 7 ? siteName.substring(0, 7).trim() : siteName;
            }


        } else if(alarmCode.equals("198094420") || alarmCode.equals("198097604") || alarmCode.equals("198200001") || alarmCode.equals("198200011")){
//            || alarmCode.equals("198094461")
            if(siteName.contains("_") ){
                siteId = siteName.substring(0, siteName.indexOf("_")).trim();
            } else {
                siteId = "RANDOM";
            }
        } else if(alarmCode.equals("198099803")){
            // Handle cases with underscore
            //empty site and missing site
            if(siteName.contains("_")){
                siteId = siteName.substring(0, siteName.indexOf("_")).trim();
            } else if(siteName.contains("(") && siteName.contains(")")){
                siteId = siteName.substring(0, siteName.indexOf("(")).trim();
            } else {
                siteId = siteName;
            }
        } else {
            String version_1_from_field_26_length_7 = siteName.substring(0, 7).trim();

            char fourthChar = siteName.charAt(3);

            if(fourthChar == 'P' || fourthChar == 'C' || fourthChar == 'U'){
                // Set siteId for 'P', 'C', or 'U' in the 4th position
                siteId = version_1_from_field_26_length_7;
            } else if(fourthChar == 'M'){
                // Check for 'M' in the 4th position
                if(siteName.charAt(4) == 'U'){
                    // If 5th char is 'U', set siteId using first 8 characters
                    siteId = siteName.substring(0, 8).trim();
                } else if(siteName.charAt(6) == '('){
                    // If 7th char is '(', set siteId using first 6 characters
                    siteId = siteName.substring(0, 6).trim();
                } else {
                    // Default case for 'M' in 4th position, use first 7 characters
                    siteId = version_1_from_field_26_length_7;
                }
            } else if(fourthChar == 'L'){
                // Set siteId for 'L' in the 4th position
                siteId = version_1_from_field_26_length_7;
            } else if(siteName.contains("_UL_")){
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

    public String setUpServiceType(String siteId, String alarmCodeString, String siteName) {


        System.out.println("****************************** service type ZTE*********************************");

        System.out.println("Site Id: " + siteId);
        System.out.println("Site Name: " + siteName);
        System.out.println("Alarm Code: " + alarmCodeString);

        System.out.println("****************************************************");

        long alarmCode;
        try {
            alarmCode = Long.parseLong(alarmCodeString);
        } catch (NumberFormatException e) {
            return "UNKNOWN"; // invalid input
        }

        if(ServiceTypes.CODES_2G.contains(alarmCode)){
            return "2G";
        } else if(ServiceTypes.CODES_3G.contains(alarmCode)){
            return "3G";
        } else if(ServiceTypes.CODES_4G.contains(alarmCode)){
            return "4G";
        } else if(ServiceTypes.CODES_2G_3G.contains(alarmCode) ||
                ServiceTypes.CODES_3G_4G.contains(alarmCode) ||
                ServiceTypes.CODES_2G_3G_4G.contains(alarmCode)){
            return ServiceTypes.checkMultiServiceTypes(siteName, siteId, alarmCode);
        } else {
            return "UNKNOWN"; // fallback for unmapped codes
        }

//        String localServiceType;
//
//        // Assume alarmCodeString is already defined as a String containing the alarm code
//        Long alarmCode = Long.parseLong(alarmCodeString);
//
//
//       // Check conditions
//        if (alarmCode == 200083023 || alarmCode == 200083022 || alarmCode == 199083023 || alarmCode == 199083022 || alarmCode == 198083023 || alarmCode == 198083022) {
//            localServiceType = "3G";
//        } else if (alarmCode == 198094422 || alarmCode == 198094420 || alarmCode == 198094419 || alarmCode == 198099803 || alarmCode == 198200011 || alarmCode == 198200001 ||
//                alarmCode == 1014 || alarmCode == 198094466 || alarmCode == 198200004) {
//            localServiceType = "4G";
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


    public String setUpDisplaySiteId(String tempSiteId, String temServiceType, String alarmRncId, String alarmNodeBId) {

        String localDisplaySiteId;

        if(alarmRncId != null && alarmNodeBId != null && temServiceType.equalsIgnoreCase("3G")){
            localDisplaySiteId = null;
        } else {
            if(temServiceType.equalsIgnoreCase("3G")){
                System.out.println("Display-3: " + tempSiteId.substring(0, 4).concat("U").concat(tempSiteId.substring(4)));

                if(tempSiteId.charAt(3) == 'M'){
                    localDisplaySiteId = tempSiteId.substring(0, 4).concat("U").concat(tempSiteId.substring(4));
                } else {
                    localDisplaySiteId = tempSiteId.substring(0, 3).concat("U").concat(tempSiteId.substring(3));
                }
            } else if(temServiceType.equalsIgnoreCase("4G")){
                localDisplaySiteId = tempSiteId.concat("_UL");
            } else {
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
        } else {
            if(inputString.length() >= 5){
                char charAt3 = inputString.charAt(3);
                char charAt4 = inputString.charAt(4);

                if(charAt3 == 'M'){
                    if(charAt4 == 'U' || charAt4 == 'P' || charAt4 == 'C' || charAt4 == 'L' || charAt4 == 'M'){
                        result = inputString.substring(0, 4) + inputString.substring(5, 8); //kblm + 100
                    } else {
                        result = inputString.substring(0, 4) + inputString.substring(4, 7); //KBLM +100
                    }

                } else {
                    if(charAt3 == 'U' || charAt3 == 'P' || charAt3 == 'C' || charAt3 == 'L'){

                        if(charAt4 == 'U' || charAt4 == 'P' || charAt4 == 'C' || charAt4 == 'L' || charAt4 == 'M'){
                            result = inputString.substring(0, 3) + inputString.substring(5, 8);
                        } else {
                            result = inputString.substring(0, 3) + inputString.substring(4, 7);
                        }
                    } else {
                        result = inputString.substring(0, 3) + inputString.substring(3, 6); //ok
                    }
                }
            }
        }


        return result;
    }


}
