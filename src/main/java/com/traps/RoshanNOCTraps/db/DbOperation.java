package com.traps.RoshanNOCTraps.db;

import com.mycompany.app.sharedClasses.HwTrapBody;
import com.mycompany.app.sharedClasses.ZteTrapBody;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class DbOperation {




    public static JdbcTemplate jdbcTemplate;


    public DbOperation(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;

    }



    public static int addZteTrap(ZteTrapBody zteTrapBody) {
        System.out.println("Here it comes insert zte");
        try{
            zteTrapBody.setId(generateUniqueId());
            String sql = """
               INSERT INTO nmt_trap_zte (
                   trap_id, alarm_code, alarm_name, site_id, site_name,
                   alarm_severity, alarm_event_type, alarm_net_type,
                   alarm_arrival_time, alarm_clear_time, alarm_fault_cause,
                   alarm_fault_class, received_on, alarm_nodeb_id,
                   alarm_cell_id, alarm_rnc_id, alarm_Service_type,
                   display_site_id, trap_uuid
               )
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
               """;

            System.out.println("ZTE INSERT COMPLETED");
            return jdbcTemplate.update(sql,
                    zteTrapBody.getTrapId(),
                    zteTrapBody.getAlarmCode(),
                    zteTrapBody.getAlarmName(),
                    zteTrapBody.getSiteId(),
                    zteTrapBody.getSiteName(),
                    zteTrapBody.getAlarmSeverity(),
                    zteTrapBody.getAlarmEventType(),
                    zteTrapBody.getAlarmNetType(),
                    zteTrapBody.getAlarmArrivalTime(),
                    zteTrapBody.getAlarmClearedTime(),
                    zteTrapBody.getAlarmFaultCause(),
                    zteTrapBody.getAlarmFaultClass(),
                    LocalDateTime.now(),
                    zteTrapBody.getAlarmNodeBId(),
                    zteTrapBody.getAlarmCellId(),
                    zteTrapBody.getAlarmRncId(),
                    zteTrapBody.getAlarmServiceType(),
                    zteTrapBody.getDisplaySiteId(),
                    zteTrapBody.getId()
            );

        }catch (Exception e){
            System.out.println("ZTE INSERT ERROR");
            e.printStackTrace();
            return -1;
        }



    }


    public static int updateZteTrap(String id, ZteTrapBody zteTrapBody) {
        System.out.println("Here it comes update zte");
        try{
            String sql = """
             UPDATE nmt_trap_zte
             SET alarm_clear_time = ?,
                 is_completed = true
             WHERE trap_id = ?
             """;
            System.out.println("ZTE UPDATE COMPLETED!!");


            return jdbcTemplate.update(sql,
                    zteTrapBody.getAlarmClearedTime(),

                    zteTrapBody.getTrapId()
            );


        }catch(Exception e){
            System.out.println("ZTE UPDATE HAS ERROR: ");
            e.printStackTrace();
            return -1;
        }


    }

    public static Long generateUniqueId() {
        UUID uuid = UUID.randomUUID();
        long mostSignificantBits = uuid.getMostSignificantBits();
        long leastSignificantBits = uuid.getLeastSignificantBits();
        return Math.abs(mostSignificantBits ^ leastSignificantBits);
    }



    public static int addHwTrap(HwTrapBody hwTrapBody) {
//        System.out.println("Here it comes insert hw");
        try{
            hwTrapBody.setId(generateUniqueId());
            String sql = """
             INSERT INTO nmt_trap_huawei (
                 trap_id, alarm_code, alarm_name, site_id, site_name,
                 alarm_severity, alarm_event_type, alarm_net_type,
                 alarm_arrival_time, alarm_clear_time, alarm_fault_cause,
                 alarm_fault_class, received_on, alarm_nodeb_id,
                 alarm_cell_id, alarm_rnc_id, alarm_Service_type,
                 display_site_id, trap_uuid
             )
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
             """;

            System.out.println("HW INSERT COMPLETED");
            return jdbcTemplate.update(sql,
                    hwTrapBody.getTrapId(),
                    hwTrapBody.getAlarmCode(),
                    hwTrapBody.getAlarmName(),
                    hwTrapBody.getSiteId(),
                    hwTrapBody.getSiteName(),
                    hwTrapBody.getAlarmSeverity(),
                    hwTrapBody.getAlarmEventType(),
                    hwTrapBody.getAlarmNetType(),
                    hwTrapBody.getAlarmArrivalTime(),
                    hwTrapBody.getAlarmClearedTime(),
                    hwTrapBody.getAlarmFaultCause(),
                    hwTrapBody.getAlarmFaultClass(),
                    LocalDateTime.now(),
                    hwTrapBody.getAlarmNodeBId(),
                    hwTrapBody.getAlarmCellId(),
                    hwTrapBody.getAlarmRncId(),
                    hwTrapBody.getAlarmServiceType(),
                    hwTrapBody.getDisplaySiteId(),
                    hwTrapBody.getId()
            );
        }catch (Exception e){
            System.out.println("HW INSERT ERROR");
            e.printStackTrace();
            return -1;

        }

    }


    public static int updateHwTrap(String id, HwTrapBody hwTrapBody) {
//        System.out.println("Here it comes update hw");
        try{
            String sql = """
             UPDATE nmt_trap_huawei
             SET alarm_clear_time = ?,
                 is_completed = true
             WHERE trap_id = ?
             """;

            System.out.println("HW UPDATE COMPLETED!!");

            return jdbcTemplate.update(sql,
                    hwTrapBody.getAlarmClearedTime(),
                   id
            );
        }catch (Exception e){
            System.out.println("HW UPDATE Error!!");
            e.printStackTrace();
            return -1;
        }

    }

}
