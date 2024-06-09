package com.traps.RoshanNOCTraps.db;

import com.traps.RoshanNOCTraps.traps.hw.HwTrapBody;
import com.traps.RoshanNOCTraps.traps.zte.ZteTrapBody;
import org.springframework.stereotype.Service;

@Service
public interface HwDao {

    int addHwTrap(HwTrapBody hwTrapBody);
    int updateHwTrap(String id, HwTrapBody hwTrapBody);

}
