package com.traps.RoshanNOCTraps.db;

import com.traps.RoshanNOCTraps.traps.zte.ZteTrapBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public class ZteDaoImpl implements ZteDoa{


    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ZteDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    @Override
    public int addZteTrap(ZteTrapBody zteTrapBody) {
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
           return 0;
       }



    }

    @Override
    public int updateZteTrap(String id, ZteTrapBody zteTrapBody) {
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
                    true,
                    zteTrapBody.getTrapId()
            );


        }catch(Exception e){
          System.out.println("ZTE UPDATE HAS ERROR: ");
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
