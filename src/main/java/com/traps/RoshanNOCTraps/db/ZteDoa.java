package com.traps.RoshanNOCTraps.db;

import com.traps.RoshanNOCTraps.traps.zte.ZteTrapBody;
import org.springframework.stereotype.Service;

@Service
public interface ZteDoa {
    int addZteTrap(ZteTrapBody zteTrapBody);
    int updateZteTrap(String id, ZteTrapBody zteTrapBody);
}
