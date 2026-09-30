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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Component
public class ProcessHwPdu {

    private static final String HW_INVESTIGATION_FOLDER =
            "HW-TRAP-INVESTIGATION";
    private static final Pattern SITE_ID_PATTERN =  Pattern.compile("^[A-Z]{3,5}\\d+$");


    public void processPdu(CommandResponderEvent crEvent) throws SQLException {
        PDU pdu = crEvent.getPDU();
        processHwPDU(pdu);
    }





    private void processHwPDU(PDU pdu) throws SQLException {
        String alarmCode = "UNKNOWN";

        try {
            if (pdu == null || pdu.getType() != PDU.TRAP) {
                return;
            }

            Long intendedAlarmHw =
                    getVariableAsLong(pdu, HwOidConstants.HW_INTENDED_ALARM);

            if (intendedAlarmHw == null ||
                    !HwOidConstants.alarmIdList.contains(intendedAlarmHw)) {
                return;
            }

            alarmCode = intendedAlarmHw.toString();

            appendHwInvestigation(
                    alarmCode,
                    "TRAP RECEIVED | " + LocalDateTime.now()
                            + " | PDU: " + pdu
            );

            appendData(
                    pdu,
                    "BSSHW-TRAPS",
                    alarmCode,
                    LocalDateTime.now()
            );

            BssHwTrapBody hwTrapBody =
                    createHuaweiTrapBody(pdu, intendedAlarmHw);

            if (hwTrapBody == null) {
                appendHwInvestigation(
                        alarmCode,
                        "BODY CREATION FAILED | " + LocalDateTime.now()
                );
                return;
            }

            appendHwInvestigation(
                    alarmCode,
                    "BODY CREATED | " + LocalDateTime.now()
                            + " | Trap ID: " + hwTrapBody.getTrapId()
                            + " | Alarm Code: " + hwTrapBody.getAlarmCode()
            );

            appendData(
                    hwTrapBody,
                    "BSSHW-TRAPS-BODIES",
                    alarmCode,
                    LocalDateTime.now()
            );

            appendHwInvestigation(
                    alarmCode,
                    "KAFKA SEND INITIATED | " + LocalDateTime.now()
                            + " | Trap ID: " + hwTrapBody.getTrapId()
                            + " | Alarm Code: " + hwTrapBody.getAlarmCode()
            );

            KafkaOperation.sendHwTrap(hwTrapBody);

            // IMPORTANT:
            // This only means sendHwTrap() returned.
            // It does NOT mean Kafka successfully received the message.
            appendHwInvestigation(
                    alarmCode,
                    "KAFKA SEND CALL COMPLETED | " + LocalDateTime.now()
                            + " | Trap ID: " + hwTrapBody.getTrapId()
            );

            logTrap(hwTrapBody);

        } catch (Exception e) {

            appendHwInvestigation(
                    alarmCode,
                    "PROCESSING EXCEPTION | " + LocalDateTime.now()
                            + " | Error: " + e
            );

            e.printStackTrace();
        }
    }

    private void appendHwInvestigation(
            String alarmCode,
            String line) {

        try {
            Path dirPath =
                    Paths.get(HW_INVESTIGATION_FOLDER);

            Files.createDirectories(dirPath);

            Path filePath =
                    dirPath.resolve(alarmCode + ".log");

            Files.write(
                    filePath,
                    (line + System.lineSeparator())
                            .getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


//    private void processHwPDU(PDU pdu) throws SQLException {
//        try {
//            if (pdu.getType() == PDU.TRAP) {
//                Long intendedAlarmHw = getVariableAsLong(pdu,HwOidConstants.HW_INTENDED_ALARM);
//                if (HwOidConstants.alarmIdList.contains(intendedAlarmHw)) {
//                    appendData(pdu,"BSSHW-TRAPS",intendedAlarmHw.toString(), LocalDateTime.now());
//                    BssHwTrapBody hwTrapBody = createHuaweiTrapBody(pdu,intendedAlarmHw);
//                    appendData(hwTrapBody,"BSSHW-TRAPS-BODIES",intendedAlarmHw.toString(),LocalDateTime.now());
//                    KafkaOperation.sendHwTrap(hwTrapBody);
//
//                    appendData(pdu,"BSSHW-TRAPS", "HW trap send initiated. Alarm ID: {}"+ hwTrapBody.getTrapId(), LocalDateTime.now());
//
//                    logTrap(hwTrapBody);
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
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
        hwTrapBody = extractAlarmSource(objectInstanceNameHw.trim(), hwTrapBody);


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


    /**
     *  OID EXTRACTION HELPERS
     */
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

    /**
     * EXTRACT TRAP SOURCE AND SERVICE INFORMATION
     */
    private BssHwTrapBody extractAlarmSource(String info, BssHwTrapBody hwTrapBody) {

        Long alarmCode = Long.parseLong(hwTrapBody.getAlarmCode());

        if (alarmCode != null) {

            switch (alarmCode.intValue()) {



                case 22214:
                    return handle22214(info, hwTrapBody,alarmCode);


                case 21807:
                    return handle21807( hwTrapBody);

                case 65080:
                    return handle65080( hwTrapBody);

                case 65070:
                    return handle65070( hwTrapBody);

                case 65071:
                    return handle65071( hwTrapBody);

                case 65501:
                    return handle65501( hwTrapBody);
                case 65033:
                    return handle65033( hwTrapBody);
                case 29201:
                    return handle29201( hwTrapBody);
                case 65069:
                    return handle65069( hwTrapBody);
                case 65090:
                    return handle65090( hwTrapBody);
                case 21805:
                    return handle21805( hwTrapBody);
                case 65084:
                    return handle65084( hwTrapBody);
                case 25622:
                    return handle25622( hwTrapBody);
                case 65067:
                    return handle65067( hwTrapBody);
                case 25621:
                    return handle25621( hwTrapBody);

                case 21825:
                    return handle21825( hwTrapBody);
                case 65050:
                    return handle65050( hwTrapBody);



                case 65060:
                    return handle65060(hwTrapBody);



                case 26108:
                    return handle26108( hwTrapBody);

                case 29213:
                    return handle29213(info, hwTrapBody);

                default:
                    return null;
            }

        } else {
            return null;
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



        String inputString = siteId;
        String result = "";

        if(inputString == null || inputString.equalsIgnoreCase("RANDOM")){
            result = siteId;
        }

        else{

            //input  = GZN028
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


    /*****
     *
     *      EXTRACTION LOGIC FOR EACH AALRM
     */

    private BssHwTrapBody handle5700( BssHwTrapBody hwTrapBody) {
        String siteId = null;
        String siteName = hwTrapBody.getSiteName();

        String untilUnderScoreOnly = null;
        String fromDashUntilUnderscore = null;


        int dashIndex = siteName.indexOf("-");
        int underscoreIndex = siteName.indexOf("_");


        if(dashIndex != -1) {
            //KBL039_MBSC-KBL133_BB_QALAI_AHMMAD_KHAN_CPX_P1; or GZNH01-GZN003_BB_PLAN_SAY_CPX_P1;
            fromDashUntilUnderscore = getSiteIdFromDashToUnderScore(siteName, dashIndex, underscoreIndex);
            siteId = fromDashUntilUnderscore;

        }else{
            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            siteId = untilUnderScoreOnly;
        }

        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);
        return hwTrapBody;
    }
    private BssHwTrapBody handle65071( BssHwTrapBody hwTrapBody) {
        String siteId = null;
        String siteName = hwTrapBody.getSiteName();

        String untilUnderScoreOnly = null;
        String fromDashUntilUnderscore = null;


        int dashIndex = siteName.indexOf("-");
        int underscoreIndex = siteName.indexOf("_");


        if(dashIndex != -1) {
            //KBL039_MBSC-KBL133_BB_QALAI_AHMMAD_KHAN_CPX_P1; or GZNH01-GZN003_BB_PLAN_SAY_CPX_P1;
            fromDashUntilUnderscore = getSiteIdFromDashToUnderScore(siteName, dashIndex, underscoreIndex);
            siteId = fromDashUntilUnderscore;

        }else{
            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            siteId = untilUnderScoreOnly;
        }

        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);
        return hwTrapBody;
    }
    private BssHwTrapBody handle22214(String info, BssHwTrapBody hwTrapBody, Long alarmCode) {

        String siteId = null;
        String siteName = null;

        //get 4th item from the info
        siteName = extractSiteNameFromInfo(info, 4,alarmCode);

        if(siteName == null) {
            return null;
        }


        if(siteName.contains("_")){
            siteId =  siteName.substring(0, siteName.indexOf("_")).trim();
        }else {
            // if there is no _, then for now we cant find site id
            return null;
        }

        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);


        return hwTrapBody;
    }
    private BssHwTrapBody handle21807( BssHwTrapBody hwTrapBody) {

        String siteId = null;
        String siteName = hwTrapBody.getSiteName();

        String untilUnderScoreOnly = null;
        String fromDashUntilUnderscore = null;

        int dashIndex = siteName.indexOf("-");
        int underscoreIndex = siteName.indexOf("_");
        int slashIndex = siteName.indexOf("/");

        if(dashIndex != -1) {
            //KBLH07-KBL413_SARAK_E_ROSA_JTTS_P3;
            fromDashUntilUnderscore = getSiteIdFromDashToUnderScore(siteName, dashIndex, underscoreIndex);
            siteId = fromDashUntilUnderscore;
        }
//        else if(slashIndex != -1) {
//            //new case: KBL039_MBSC/KBL246_SHAIKH_MUHAMMADI_JTTS_P3; expected format: KBL039_MBSC-KBL246_SHAIKH_MUHAMMADI_JTTS_P3
//            fromDashUntilUnderscore = getFromSlashTillUnderscorePartOfAString(siteName, slashIndex);
//            siteId = fromDashUntilUnderscore;
//        }

        else{

            //case1: KBLH07-KBL413_SARAK_E_ROSA_JTTS_P3;
            //case2: KBL039_MBSC-KBL112_BAGRAMI_AWCC(KBL373)_P2;

            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            siteId = untilUnderScoreOnly;
        }


        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);



        return hwTrapBody;
    }
    private BssHwTrapBody handle65080( BssHwTrapBody hwTrapBody) {

        String siteId = null;
        String siteName = hwTrapBody.getSiteName();

        String untilUnderScoreOnly = null;
        String fromDashUntilUnderscore = null;


        int dashIndex = siteName.indexOf("-");
        int underscoreIndex = siteName.indexOf("_");

        if(dashIndex != -1) {
            //MZRH03-MZR001_MAZAR_MOC_CPX_P3;
            fromDashUntilUnderscore = getSiteIdFromDashToUnderScore(siteName, dashIndex, underscoreIndex);
            siteId = fromDashUntilUnderscore;

        }else{
            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            siteId = untilUnderScoreOnly;
        }



        //other specific part
        if(siteId.equalsIgnoreCase("E-CENA") || siteId.equalsIgnoreCase("e-Khis")){
            System.out.println("Found: "+hwTrapBody.toString());
        }



        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);
        return hwTrapBody;
    }
    private BssHwTrapBody handle65070( BssHwTrapBody hwTrapBody) {
        String siteId = null;
        String siteName = hwTrapBody.getSiteName();

        String untilUnderScoreOnly = null;
        String fromDashUntilUnderscore = null;


        int dashIndex = siteName.indexOf("-");
        int underscoreIndex = siteName.indexOf("_");


        if(dashIndex != -1) {
            //KBL039_MBSC-KBL133_BB_QALAI_AHMMAD_KHAN_CPX_P1; or GZNH01-GZN003_BB_PLAN_SAY_CPX_P1;
            fromDashUntilUnderscore = getSiteIdFromDashToUnderScore(siteName, dashIndex, underscoreIndex);
            siteId = fromDashUntilUnderscore;

        }else{
            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            siteId = untilUnderScoreOnly;
        }

        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);
        return hwTrapBody;
    }
    private BssHwTrapBody handle65501( BssHwTrapBody hwTrapBody) {
        String siteId = null;
        String siteName = hwTrapBody.getSiteName();

        String untilUnderScoreOnly = null;
        String fromDashUntilUnderscore = null;


        int dashIndex = siteName.indexOf("-");
        int underscoreIndex = siteName.indexOf("_");


        if(dashIndex != -1) {
            //KBL039_MBSC-KBL133_BB_QALAI_AHMMAD_KHAN_CPX_P1; or GZNH01-GZN003_BB_PLAN_SAY_CPX_P1;
            fromDashUntilUnderscore = getSiteIdFromDashToUnderScore(siteName, dashIndex, underscoreIndex);
            siteId = fromDashUntilUnderscore;

        }else{
            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            siteId = untilUnderScoreOnly;
        }

        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);
        return hwTrapBody;
    }
    private BssHwTrapBody handle65033( BssHwTrapBody hwTrapBody) {

        String siteId = null;
        String siteName = hwTrapBody.getSiteName();

        String untilUnderScoreOnly = null;
        String fromDashUntilUnderscore = null;


        int dashIndex = siteName.indexOf("-");
        int underscoreIndex = siteName.indexOf("_");

        if(dashIndex != -1) {
            //KDZH01-KDZ016_QALA_I_ZAL_CPX_P1;
            fromDashUntilUnderscore = getSiteIdFromDashToUnderScore(siteName, dashIndex, underscoreIndex);
            siteId = fromDashUntilUnderscore;

        }else{
            //KBL485_UL_PT;
            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            siteId = untilUnderScoreOnly;
        }

        if(siteId.contains("-")){
            siteId = untilUnderScoreOnly;
        }

        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);
        return hwTrapBody;

    }
    private BssHwTrapBody handle29201( BssHwTrapBody hwTrapBody) {
        String siteId = null;
        String siteName = hwTrapBody.getSiteName();

        String untilUnderScoreOnly = null;
        String fromDashUntilUnderscore = null;


        int dashIndex = siteName.indexOf("-");
        int underscoreIndex = siteName.indexOf("_");

        if(dashIndex != -1) {
            //MZRH03-MZR001_MAZAR_MOC_CPX_P3;
            fromDashUntilUnderscore = getSiteIdFromDashToUnderScore(siteName, dashIndex, underscoreIndex);
            siteId = fromDashUntilUnderscore;

        }else{
            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            siteId = untilUnderScoreOnly;
        }



        //other specific part
        if(siteId.equalsIgnoreCase("E-CENA") || siteId.equalsIgnoreCase("e-Khis")){
            System.out.println("Found: "+hwTrapBody.toString());
        }



        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);
        return hwTrapBody;
    }
    private BssHwTrapBody handle65059( BssHwTrapBody hwTrapBody) {
        String siteId = null;
        String siteName = hwTrapBody.getSiteName();

        String untilUnderScoreOnly = null;
        String fromDashUntilUnderscore = null;


        int dashIndex = siteName.indexOf("-");
        int underscoreIndex = siteName.indexOf("_");

        if(dashIndex != -1) {
            //MZRH03-MZR001_MAZAR_MOC_CPX_P3;
            fromDashUntilUnderscore = getSiteIdFromDashToUnderScore(siteName, dashIndex, underscoreIndex);
            siteId = fromDashUntilUnderscore;

        }else{
            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            siteId = untilUnderScoreOnly;
        }



        //other specific part
        if(siteId.equalsIgnoreCase("E-CENA") || siteId.equalsIgnoreCase("e-Khis")){
            System.out.println("Found: "+hwTrapBody.toString());
        }



        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);
        return hwTrapBody;
    }
    private BssHwTrapBody handle65068(  BssHwTrapBody hwTrapBody) {
        String siteId = null;
        String siteName = hwTrapBody.getSiteName();

        String untilUnderScoreOnly = null;
        String fromDashUntilUnderscore = null;


        int dashIndex = siteName.indexOf("-");
        int underscoreIndex = siteName.indexOf("_");

        if(dashIndex != -1) {
            //MZRH03-MZR001_MAZAR_MOC_CPX_P3;
            fromDashUntilUnderscore = getSiteIdFromDashToUnderScore(siteName, dashIndex, underscoreIndex);
            siteId = fromDashUntilUnderscore;

        }else{
            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            siteId = untilUnderScoreOnly;
        }



        //other specific part
        if(siteId.equalsIgnoreCase("E-CENA") || siteId.equalsIgnoreCase("e-Khis")){
            System.out.println("Found: "+hwTrapBody.toString());
        }



        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);
        return hwTrapBody;
    }
    private BssHwTrapBody handle65069(  BssHwTrapBody hwTrapBody) {
        String siteId = null;
        String siteName = hwTrapBody.getSiteName();

        String untilUnderScoreOnly = null;
        String fromDashUntilUnderscore = null;


        int dashIndex = siteName.indexOf("-");
        int underscoreIndex = siteName.indexOf("_");

        if(dashIndex != -1) {
            //MZRH03-MZR001_MAZAR_MOC_CPX_P3;
            fromDashUntilUnderscore = getSiteIdFromDashToUnderScore(siteName, dashIndex, underscoreIndex);
            siteId = fromDashUntilUnderscore;

        }else{
            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            siteId = untilUnderScoreOnly;
        }



        //other specific part
        if(siteId.equalsIgnoreCase("E-CENA") || siteId.equalsIgnoreCase("e-Khis")){
            System.out.println("Found: "+hwTrapBody.toString());
        }



        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);
        return hwTrapBody;
    }
    private BssHwTrapBody handle21805(  BssHwTrapBody hwTrapBody) {
        String siteId = null;
        String siteName = hwTrapBody.getSiteName();

        String untilUnderScoreOnly = null;
        String fromDashUntilUnderscore = null;


        int dashIndex = siteName.indexOf("-");
        int underscoreIndex = siteName.indexOf("_");

        if(dashIndex != -1) {
            //MZRH03-MZR001_MAZAR_MOC_CPX_P3;
            fromDashUntilUnderscore = getSiteIdFromDashToUnderScore(siteName, dashIndex, underscoreIndex);
            siteId = fromDashUntilUnderscore;

        }else{
            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            siteId = untilUnderScoreOnly;
        }



        //other specific part
        if(siteId.equalsIgnoreCase("E-CENA") || siteId.equalsIgnoreCase("e-Khis")){
            System.out.println("Found: "+hwTrapBody.toString());
        }



        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);
        return hwTrapBody;
    }
    private BssHwTrapBody handle65084(  BssHwTrapBody hwTrapBody) {
        String siteId = null;
        String siteName = hwTrapBody.getSiteName();

        String untilUnderScoreOnly = null;
        String fromDashUntilUnderscore = null;


        int dashIndex = siteName.indexOf("-");
        int underscoreIndex = siteName.indexOf("_");

        if(dashIndex != -1) {
            //MZRH03-MZR001_MAZAR_MOC_CPX_P3;
            fromDashUntilUnderscore = getSiteIdFromDashToUnderScore(siteName, dashIndex, underscoreIndex);
            siteId = fromDashUntilUnderscore;

        }else{
            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            siteId = untilUnderScoreOnly;
        }



        //other specific part
        if(siteId.equalsIgnoreCase("E-CENA") || siteId.equalsIgnoreCase("e-Khis")){
            System.out.println("Found: "+hwTrapBody.toString());
        }



        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);
        return hwTrapBody;
    }
    private BssHwTrapBody handle65090(  BssHwTrapBody hwTrapBody) {
        String siteId = null;
        String siteName = hwTrapBody.getSiteName();

        String untilUnderScoreOnly = null;
        String fromDashUntilUnderscore = null;


        int dashIndex = siteName.indexOf("-");
        int underscoreIndex = siteName.indexOf("_");

        if(dashIndex != -1) {
            //MZRH03-MZR001_MAZAR_MOC_CPX_P3;
            fromDashUntilUnderscore = getSiteIdFromDashToUnderScore(siteName, dashIndex, underscoreIndex);
            siteId = fromDashUntilUnderscore;

        }else{
            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            siteId = untilUnderScoreOnly;
        }



        //other specific part
        if(siteId.equalsIgnoreCase("E-CENA") || siteId.equalsIgnoreCase("e-Khis")){
            System.out.println("Found: "+hwTrapBody.toString());
        }



        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);
        return hwTrapBody;
    }
    private BssHwTrapBody handle25622( BssHwTrapBody hwTrapBody) {


        String siteId = null;
        String siteName = hwTrapBody.getSiteName();

        String untilUnderScoreOnly = null;
        String fromDashUntilUnderscore = null;



        int dashIndex = siteName.indexOf("-");
        int underscoreIndex = siteName.indexOf("_");

//        MZRU087_KART-E-AMANI_Etisalat(BKH031)_P3

//        if(da){
//
//        }

        if(dashIndex != -1) {
            //KBLH12-KBL148_FRENCH_HOSPITAL_CPX_P2
            fromDashUntilUnderscore = getSiteIdFromDashToUnderScore(siteName, dashIndex, underscoreIndex);
            siteId = fromDashUntilUnderscore;

        }else{
            //KBL313_3G only_CPX; //GZN028_GL
            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            siteId = untilUnderScoreOnly;
        }


        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);
        return hwTrapBody;


    }
    private BssHwTrapBody handle65067( BssHwTrapBody hwTrapBody) {

        String siteId = null;
        String siteName = hwTrapBody.getSiteName();

        String untilUnderScoreOnly = null;
        int underscoreIndex = siteName.indexOf("_");


//        KBLU168_SYEED_OMAR_Market_Pul-e-Khishti_CPX_P2;
//        BMN011_UL_CPX;
//        KBLU107_ZOR_ABAD_CPX_P1;
//        BMNU046_Lampsite BMN-Hospital;

        if(underscoreIndex != -1) {
            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            siteId = untilUnderScoreOnly;
        }else {
            return null;
        }



        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);
        return hwTrapBody;


    }
    private BssHwTrapBody handle25621(BssHwTrapBody hwTrapBody) {

        String siteId = null;
        String siteName = hwTrapBody.getSiteName();

        String untilUnderScoreOnly = null;
        String fromDashUntilUnderscore = null;

        int dashIndex = siteName.indexOf("-");
        int underscoreIndex = siteName.indexOf("_");

//        KBLH07-KBL361_500_FAMILY_KHAIR_KHANA_JTTS_P3;
//        KBL039_MBSC-KBL246_SHAIKH_MUHAMMADI_JTTS_P3;
//        KBL177_UL_TXN_JTTS;
        if(dashIndex != -1) {
            fromDashUntilUnderscore = getSiteIdFromDashToUnderScore(siteName, dashIndex, underscoreIndex);
            siteId = fromDashUntilUnderscore;

        }else{
            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            siteId = untilUnderScoreOnly;
        }

        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);
        return hwTrapBody;

    }
    private BssHwTrapBody handle21825( BssHwTrapBody hwTrapBody) {
        String siteId = null;
        String siteName = hwTrapBody.getSiteName();

        String untilUnderScoreOnly = null;
        String fromDashUntilUnderscore = null;


        int dashIndex = siteName.indexOf("-");
        int underscoreIndex = siteName.indexOf("_");


        if(dashIndex != -1) {
            //KBL039_MBSC-KBL133_BB_QALAI_AHMMAD_KHAN_CPX_P1; or GZNH01-GZN003_BB_PLAN_SAY_CPX_P1;
            fromDashUntilUnderscore = getSiteIdFromDashToUnderScore(siteName, dashIndex, underscoreIndex);
            siteId = fromDashUntilUnderscore;

        }else{
            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            siteId = untilUnderScoreOnly;
        }

        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);
        return hwTrapBody;
    }
    private BssHwTrapBody handle65050( BssHwTrapBody hwTrapBody) {



        String siteId = null;
        String siteName = hwTrapBody.getSiteName();

        String untilUnderScoreOnly = null;
        String fromDashUntilUnderscore = null;


        int dashIndex = siteName.indexOf("-");
        int underscoreIndex = siteName.indexOf("_");


        if(dashIndex != -1) {
            //KBL039_MBSC-KBL133_BB_QALAI_AHMMAD_KHAN_CPX_P1; or GZNH01-GZN003_BB_PLAN_SAY_CPX_P1;
            fromDashUntilUnderscore = getSiteIdFromDashToUnderScore(siteName, dashIndex, underscoreIndex);
            siteId = fromDashUntilUnderscore;

        }else{
            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            siteId = untilUnderScoreOnly;
        }

        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);

        return hwTrapBody;
    }
    private BssHwTrapBody handle65060( BssHwTrapBody hwTrapBody) {
        String siteId = null;
        String siteName = hwTrapBody.getSiteName();

        String untilUnderScoreOnly = null;
        String fromDashUntilUnderscore = null;


        int dashIndex = siteName.indexOf("-");
        int underscoreIndex = siteName.indexOf("_");


        if(dashIndex != -1) {
            //KBL039_MBSC-KBL133_BB_QALAI_AHMMAD_KHAN_CPX_P1; or GZNH01-GZN003_BB_PLAN_SAY_CPX_P1;
            fromDashUntilUnderscore = getSiteIdFromDashToUnderScore(siteName, dashIndex, underscoreIndex);
            siteId = fromDashUntilUnderscore;

        }else{
            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            siteId = untilUnderScoreOnly;
        }

        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);

        return hwTrapBody;
    }
    private BssHwTrapBody handle26108( BssHwTrapBody hwTrapBody) {


        String siteId = null;
        String siteName = hwTrapBody.getSiteName();

        String untilUnderScoreOnly = null;

        int underscoreIndex = siteName.indexOf("_");

//        KBLMU373_DARYA_VILLAGE_CPX_P1;
        if(underscoreIndex != -1) {
            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            siteId = untilUnderScoreOnly;

        }else{
          return null;
        }

        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);

        return hwTrapBody;
    }
    private BssHwTrapBody handle29213(String info, BssHwTrapBody hwTrapBody) {
        String siteId = null;
        String siteName = hwTrapBody.getSiteName();

        String untilUnderScoreOnly = null;

        int underscoreIndex = siteName.indexOf("_");

//        KBLMU373_DARYA_VILLAGE_CPX_P1;
        if(underscoreIndex != -1) {
            untilUnderScoreOnly = getUntilUnderscorePartOfAString(siteName,underscoreIndex);
            siteId = untilUnderScoreOnly;

        }else{
            return null;
        }

        //set the data to object
        hwTrapBody.setSiteId(siteId);
        hwTrapBody.setSiteName(siteName);

        return hwTrapBody;
    }
    private String extractSiteNameFromInfo(String info, int index , Long code) {

        String[] arr = info.split(",");



        // Ensure the index is within bounds; default to index 2 if out of range
        if (index < 0 || index >= arr.length) {

            if(code ==22214L){
                // we have either 8 size info or 3 size info
                //if 3 then site = 2 else site = 4
                index = 2;
            }else{
                index = arr.length - 2;
            }

        }




        if(code == 21541L){
            index = arr.length - 2;
        }




        // If index 2 is still out of bounds, return an empty string
        if (index >= arr.length) {
            return null;
        }


        //extract the term from the array by index

        String term = arr[index].trim();

        // Ensure the term contains "=" before attempting substring extraction
        int equalsIndex = term.indexOf("=");
        if (equalsIndex == -1 || equalsIndex == term.length() - 1) {
            return null;
        }

        return term.substring(equalsIndex + 1).trim();
    }


    /***
     * OREVIOUS VERSION OF EXTRACTION
     */
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
            fromDashUntilUnderscore = getSiteIdFromDashToUnderScore(siteName, dashIndex, underscoreIndex);
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
                    siteId = resolveSiteIdVersion2(siteName, untilUnderScoreOnly, fromDashUntilUnderscore, ifHas_UL_From0Until_UL_);

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

    private static String getSiteIdFromDashToUnderScore(String siteName, int dashIndex, int underscoreIndex) {


        //MZRU087_KART-E-AMANI_Etisalat(BKH031)_P3
        String fromDashUntilUnderscore = null;

        if (dashIndex != -1 && underscoreIndex != -1) {
            // Case 1: Extract the substring between the dash and underscore
            if (dashIndex < underscoreIndex) {
                // Case 1: First dash before underscore
                fromDashUntilUnderscore = siteName.substring(dashIndex + 1, underscoreIndex).trim();
            } else {


                 fromDashUntilUnderscore = siteName.substring(0, underscoreIndex).trim();

                if (SITE_ID_PATTERN.matcher(fromDashUntilUnderscore).matches()) {
                    return fromDashUntilUnderscore;
                }else{
                    // Case 2: If underscore appears before dash, adjust extraction
                    // Extracting from dash to next underscore (we assume the next underscore is after the first dash)
                    int nextUnderscoreIndex = siteName.indexOf("_", dashIndex);
                    if (nextUnderscoreIndex != -1) {
                        fromDashUntilUnderscore = siteName.substring(dashIndex + 1, nextUnderscoreIndex).trim();

                        if (SITE_ID_PATTERN.matcher(fromDashUntilUnderscore).matches()) {
                            return fromDashUntilUnderscore;
                        }
                    }
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

    private String getFromSlashTillUnderscorePartOfAString(String siteName, int slashIndex) {
        if (siteName == null || slashIndex == -1) {
            return null;
        }

        int nextUnderscoreIndex = siteName.indexOf("_", slashIndex + 1);

        if (nextUnderscoreIndex == -1) {
            return null;
        }

        String siteId = siteName.substring(slashIndex + 1, nextUnderscoreIndex).trim();

        if (SITE_ID_PATTERN.matcher(siteId).matches()) {
            return siteId;
        }

        return "RANDOM";
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

        // If index 2 is still out of bounds, return an empty string
        if (index >= arr.length) {
            return null;
        }


        String term = arr[index].trim();

        // Ensure the term contains "=" before attempting substring extraction
        int equalsIndex = term.indexOf("=");
        if (equalsIndex == -1 || equalsIndex == term.length() - 1) {
            return "";
        }

        return term.substring(equalsIndex + 1).trim();
    }
    // Helper method to extract site ID from site name
    private String extractSiteIdFromSiteName(String siteName, String alarmCodeHW) {

        if(siteName.contains("_")){
            return siteName.substring(0, siteName.indexOf("_")).trim();
        }

        return siteName;
    }
    private String resolveSiteIdVersion2(String siteName, String untilUnderScoreOnly, String fromDashUntilUnderscore, String ifHas_UL_From0Until_UL_) {

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

    /***
     * APPEND DATA TO FILES FOR TESTING PURPOSES
     */
    public void appendData(PDU pdu, String folder, String fileName, LocalDateTime now) {
        try {
            // Ensure the directory exists
            Path dirPath = Paths.get(folder);

            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // Construct full file path
            Path filePath = dirPath.resolve(fileName);

            String line = now + " - " + pdu + System.lineSeparator();

            // Write data to the file
            Files.write(
                    filePath,
                    line.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void appendData(BssHwTrapBody pdu, String folderName, String fileName, LocalDateTime now) {
        try {
            Path folderPath = Paths.get(folderName);

            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath); // Ensure the folder exists
            }

            Path filePath = folderPath.resolve(fileName);

            String line = now + " - " + pdu + System.lineSeparator();

            Files.write(
                    filePath,
                    line.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

        } catch (IOException e) {
            e.printStackTrace();
        }
    }




}
