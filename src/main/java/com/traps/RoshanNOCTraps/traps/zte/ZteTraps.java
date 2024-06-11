package com.traps.RoshanNOCTraps.traps.zte;

import com.traps.RoshanNOCTraps.traps.hw.ProcessHwPdu;
import com.traps.RoshanNOCTraps.traps.trapsConfig.TrapsConfig;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.snmp4j.*;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.*;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;


public class ZteTraps extends TrapsConfig implements CommandResponder {



    public void run() {
        try {
            init();
            snmp.addCommandResponder(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private void init() throws UnknownHostException, IOException {


        threadPool = ThreadPool.create("Trap", 10);
        dispatcher = new MultiThreadedMessageDispatcher(threadPool,
                new MessageDispatcherImpl());

        //TRANSPORT
//        listenAddress = GenericAddress.parse(System.getProperty(
//                "snmp4j.listenAddress", "udp:192.168.25.125/162"));


//
           listenAddress = GenericAddress.parse(System.getProperty(
             "snmp4j.listenAddress", "udp:10.150.150.35/162"));



        TransportMapping<?> transport;
        if (listenAddress instanceof UdpAddress) {
            transport = new DefaultUdpTransportMapping(
                    (UdpAddress) listenAddress);
        } else {
            transport = new DefaultTcpTransportMapping(
                    (TcpAddress) listenAddress);
        }

        //V3 SECURITY
        USM usm = new USM(
                SecurityProtocols.getInstance().addDefaultProtocols(),
                new OctetString(MPv3.createLocalEngineID()), 0);

        SecurityProtocols.getInstance().addPrivacyProtocol(new PrivAES192());
        SecurityProtocols.getInstance().addPrivacyProtocol(new PrivAES256());
        SecurityProtocols.getInstance().addPrivacyProtocol(new Priv3DES());
        usm.setEngineDiscoveryEnabled(true);
        SecurityModels.getInstance().addSecurityModel(usm);

        snmp = new Snmp(dispatcher, transport);
        snmp.getMessageDispatcher().addMessageProcessingModel(new MPv1());
        snmp.getMessageDispatcher().addMessageProcessingModel(new MPv2c());
        snmp.getMessageDispatcher().addMessageProcessingModel(new MPv3(usm));


        //check accourding to the manual of ZTE UMS
        String username = "username";
        String authpassphrase = "authpassphrase";
        String privacypassphrase = "privacypassphrase";

        snmp.getUSM().addUser(
                new OctetString(username),
                new UsmUser(new OctetString(username),AuthMD5.ID, new OctetString(
                        authpassphrase), PrivAES128.ID, new OctetString(privacypassphrase)));


        snmp.listen();
    }


    @Override
    public void processPdu(CommandResponderEvent crEvent) {
        try {

//            processZtePdu.processPdu(crEvent);
            new ProcessZtePdu().processPdu(crEvent);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
