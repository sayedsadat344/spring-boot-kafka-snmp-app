package com.traps.RoshanNOCTraps.db;


import com.mycompany.app.sharedClasses.BssZteTrapBody;
import org.springframework.stereotype.Service;

@Service
public interface ZteDoa {
    int addZteTrap(BssZteTrapBody zteTrapBody);
    int updateZteTrap(String id, BssZteTrapBody zteTrapBody);
}
