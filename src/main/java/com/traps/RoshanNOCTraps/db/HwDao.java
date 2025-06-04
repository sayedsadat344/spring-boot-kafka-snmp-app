package com.traps.RoshanNOCTraps.db;


import com.mycompany.app.sharedClasses.BssHwTrapBody;
import org.springframework.stereotype.Service;

@Service
public interface HwDao {

    int addHwTrap(BssHwTrapBody hwTrapBody);
    int updateHwTrap(String id, BssHwTrapBody hwTrapBody);

}
