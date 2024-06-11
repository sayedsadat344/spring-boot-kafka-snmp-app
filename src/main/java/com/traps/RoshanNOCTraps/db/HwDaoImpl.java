package com.traps.RoshanNOCTraps.db;

import com.mycompany.app.sharedClasses.HwTrapBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public class HwDaoImpl implements HwDao{


    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public HwDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int addHwTrap(HwTrapBody hwTrapBody) {
        System.out.println("Here it comes insert hw");
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
            return 0;

        }

    }

    @Override
    public int updateHwTrap(String id, HwTrapBody hwTrapBody) {
        System.out.println("Here it comes update hw");
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
                   true,
                   hwTrapBody.getTrapId()
           );
       }catch (Exception e){
           System.out.println("HW UPDATE Error!!");
           e.printStackTrace();
           return 0;
       }

    }

    public static Long generateUniqueId() {
        UUID uuid = UUID.randomUUID();
        long mostSignificantBits = uuid.getMostSignificantBits();
        long leastSignificantBits = uuid.getLeastSignificantBits();
        return Math.abs(mostSignificantBits ^ leastSignificantBits);
    }

}
