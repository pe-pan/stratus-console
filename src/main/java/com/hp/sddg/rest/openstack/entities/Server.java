package com.hp.sddg.rest.openstack.entities;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Created by panuska on 26.9.14.
 */
public class Server extends OpenStackEntity {

    public Server(Object node) {
        init(node);
        context = "server";    //todo merge with handler context
    }

    @Override
    protected void init(Object o) {
        super.init(o);
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();

        try {
            NodeList addresses = (NodeList) xpath.evaluate("addresses/network/ip/@addr", o, XPathConstants.NODESET);
            if (addresses != null) {
                for (int i = 0; i < addresses.getLength(); i++) {
                    properties.put("address_"+i, addresses.item(i).getTextContent());
                }
            }
        } catch (XPathExpressionException e) {
            throw new IllegalStateException(e);
        }

    }
}
