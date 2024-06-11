package com.traps.RoshanNOCTraps.db;

import com.mycompany.app.sharedClasses.HwTrapBody;
import org.springframework.stereotype.Service;

@Service
public interface HwDao {

    int addHwTrap(HwTrapBody hwTrapBody);
    int updateHwTrap(String id, HwTrapBody hwTrapBody);

}
