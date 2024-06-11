package com.traps.RoshanNOCTraps.db;

import com.mycompany.app.sharedClasses.ZteTrapBody;
import org.springframework.stereotype.Service;

@Service
public interface ZteDoa {
    int addZteTrap(ZteTrapBody zteTrapBody);
    int updateZteTrap(String id, ZteTrapBody zteTrapBody);
}
