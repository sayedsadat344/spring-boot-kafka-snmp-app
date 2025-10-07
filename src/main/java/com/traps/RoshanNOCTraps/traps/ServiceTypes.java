package com.traps.RoshanNOCTraps.traps;

import java.util.Set;

public class ServiceTypes {

    public static final Set<Long> CODES_3G = Set.of(
            1014L, 18606L, 22214L, 198066029L, 198083023L, 199083023L,
            199407043L, 200083023L, 25621L, 65067L, 65069L, 65080L,
            65334L, 65368L, 65381L, 198092295L
    );

    public static final Set<Long> CODES_4G = Set.of(
            29201L, 29840L, 29249L, 29213L, 198094420L,
            198094422L, 198094466L, 198200001L, 198200004L, 198200011L
    );


    public static final Set<Long> CODES_2G = Set.of(
            21807L, 198087337L, 199087337L, 200087337L, 897589254L,
            198083022L, 25622L, 65084L, 65090L, 65501L, 65337L,
            198092562L, 198092551L
    );


    public static final Set<Long> CODES_2G_3G_4G = Set.of(
            198092550L, 198092552L, 65033L
    );


    public static final Set<Long> CODES_3G_4G = Set.of(
            21825L, 198097604L, 198099803L, 26108L
    );

    public static final Set<Long> CODES_2G_3G = Set.of(
            65050L
    );


    public static String checkMultiServiceTypes(String siteName,String siteId,Long alarmId){

        if(siteId.charAt(3) == 'U'){
            return "3G";
        }else  if(siteId.charAt(3) == 'M' && siteId.charAt(4) == 'U'){
            return "3G";
        } else if(siteName.contains("_3G")){
            return "3G";
        } else if(siteName.contains(siteId+"_UL")){
            return "4G";
        }else{
            return "2G";
        }

    }
}
