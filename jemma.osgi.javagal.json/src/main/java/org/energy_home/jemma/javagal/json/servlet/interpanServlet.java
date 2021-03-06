/**
 * This file is part of JEMMA - http://jemma.energy-home.org
 * (C) Copyright 2013 Telecom Italia (http://www.telecomitalia.it)
 *
 * JEMMA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License (LGPL) version 3
 * or later as published by the Free Software Foundation, which accompanies
 * this distribution and is available at http://www.gnu.org/licenses/lgpl.html
 *
 * JEMMA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License (LGPL) for more details.
 *
 */
package org.energy_home.jemma.javagal.json.servlet;

import java.io.IOException;
import java.math.BigInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.energy_home.jemma.javagal.json.constants.Resources;
import org.energy_home.jemma.javagal.json.util.Util;
import org.energy_home.jemma.zgd.GalExtenderProxy;
import org.energy_home.jemma.zgd.GalExtenderProxyFactory;
import org.energy_home.jemma.zgd.GatewayConstants;
import org.energy_home.jemma.zgd.GatewayException;
import org.energy_home.jemma.zgd.GatewayInterface;
import org.energy_home.jemma.zgd.jaxb.Address;
import org.energy_home.jemma.zgd.jaxb.Info;
import org.energy_home.jemma.zgd.jaxb.InterPANMessage;
import org.energy_home.jemma.zgd.jaxb.NodeDescriptor;
import org.energy_home.jemma.zgd.jaxb.NodeServices;
import org.energy_home.jemma.zgd.jaxb.Info.Detail;
import org.energy_home.jemma.zgd.jaxb.Status;
import org.energy_home.jemma.zgd.jaxb.WSNNodeList;

import com.google.gson.Gson;

public class interpanServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	GatewayInterface gatewayInterface;
	Gson gson;

	public interpanServlet(GatewayInterface _gatewayInterface) {
		gatewayInterface = _gatewayInterface;
		gson = new Gson();
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		HttpSession session = request.getSession(true);
		Object done = session.getValue("javaGallogon.isDone");
		if (done != null) {

			String timeoutString = null;

			Long timeout = -1l;

			Object timeoutParam = request
					.getParameter(Resources.URI_PARAM_TIMEOUT);

			if (timeoutParam == null) {
				Info info = new Info();
				Status _st = new Status();
				_st.setCode((short) GatewayConstants.GENERAL_ERROR);
				_st.setMessage("Error: mandatory '"
						+ Resources.URI_PARAM_TIMEOUT + "' parameter missing.");
				info.setStatus(_st);
				Info.Detail detail = new Info.Detail();
				info.setDetail(detail);
				response.getOutputStream().print(gson.toJson(info));
				return;

			} else {
				timeoutString = timeoutParam.toString();
				if (!timeoutString.toLowerCase().startsWith("0x"))
					timeoutString = "0x" + timeoutString;
				try {
					timeout = Long.decode(timeoutString);
					if (!Util.isUnsigned32(timeout)) {

						Info info = new Info();
						Status _st = new Status();
						_st.setCode((short) GatewayConstants.GENERAL_ERROR);
						_st.setMessage("Error: mandatory '"
								+ Resources.URI_PARAM_TIMEOUT
								+ "' parameter's value invalid. You provided: "
								+ timeoutString);
						info.setStatus(_st);
						Info.Detail detail = new Info.Detail();
						info.setDetail(detail);
						response.getOutputStream().print(gson.toJson(info));
						return;

					}
				} catch (NumberFormatException nfe) {

					Info info = new Info();
					Status _st = new Status();
					_st.setCode((short) GatewayConstants.GENERAL_ERROR);
					_st.setMessage(nfe.getMessage());
					info.setStatus(_st);
					Info.Detail detail = new Info.Detail();
					info.setDetail(detail);
					response.getOutputStream().print(gson.toJson(info));
					return;

				}
			}
			Address addressObj = new Address();
			Object addressParam = request.getParameter(Resources.URI_ADDR);
			if (addressParam == null) {
				Info info = new Info();
				Status _st = new Status();
				_st.setCode((short) GatewayConstants.GENERAL_ERROR);
				_st.setMessage("Error: mandatory '" + Resources.URI_ADDR
						+ "' parameter missing.");
				info.setStatus(_st);
				Info.Detail detail = new Info.Detail();
				info.setDetail(detail);
				response.getOutputStream().print(gson.toJson(info));
				return;

			} else {
				String addrString = addressParam.toString();

				if (addrString.length() == 16) {
					BigInteger addressBigInteger = BigInteger.valueOf(Long
							.parseLong(addrString, 16));
					addressObj.setIeeeAddress(addressBigInteger);
				} else {
					Info info = new Info();
					Status _st = new Status();
					_st.setCode((short) GatewayConstants.GENERAL_ERROR);
					_st.setMessage("Wrong Address parameter");
					info.setStatus(_st);
					Info.Detail detail = new Info.Detail();
					info.setDetail(detail);
					response.getOutputStream().print(gson.toJson(info));
					return;

				}

			}
			Object panIDParam = request.getParameter(Resources.URI_PANID);
			Address panIDParamObj = new Address();
			if (panIDParam == null) {
				Info info = new Info();
				Status _st = new Status();
				_st.setCode((short) GatewayConstants.GENERAL_ERROR);
				_st.setMessage("Error: mandatory '" + Resources.URI_PANID
						+ "' parameter missing.");
				info.setStatus(_st);
				Info.Detail detail = new Info.Detail();
				info.setDetail(detail);
				response.getOutputStream().print(gson.toJson(info));
				return;

			} else {
				String panIDString = panIDParam.toString();

				if (panIDString.length() == 4) {
					Integer addressInteger = Integer.parseInt(panIDString, 16);
					panIDParamObj.setNetworkAddress(addressInteger);
				} else {
					Info info = new Info();
					Status _st = new Status();
					_st.setCode((short) GatewayConstants.GENERAL_ERROR);
					_st.setMessage("Wrong panId parameter");
					info.setStatus(_st);
					Info.Detail detail = new Info.Detail();
					info.setDetail(detail);
					response.getOutputStream().print(gson.toJson(info));
					return;

				}
			}
			Object asduParam = request.getParameter(Resources.ASDU);
			byte[] asdu = null;
			if (asduParam == null) {
				Info info = new Info();
				Status _st = new Status();
				_st.setCode((short) GatewayConstants.GENERAL_ERROR);
				_st.setMessage("Error: mandatory '" + Resources.ASDU
						+ "' parameter missing.");
				info.setStatus(_st);
				Info.Detail detail = new Info.Detail();
				info.setDetail(detail);
				response.getOutputStream().print(gson.toJson(info));
				return;

			} else {

				String asduString = asduParam.toString();
				asdu = asduString.getBytes();
			}
			try {
				InterPANMessage m = new InterPANMessage();
				m.setSrcAddressMode(3);
				m.setSrcAddress(addressObj);
				m.setSrcPANID(panIDParamObj.getNetworkAddress());

				m.setDstAddressMode(2);
				Address broadcast = new Address();
				broadcast.setNetworkAddress(0xFFFF);
				m.setDestinationAddress(broadcast);
				m.setDestPANID(broadcast.getNetworkAddress());

				m.setProfileID(49246);
				m.setClusterID(4096);
				m.setASDULength(asdu.length);
				m.setASDU(asdu);

				gatewayInterface.sendInterPANMessage(timeout, m);

				Status status = new Status();
				status.setCode((short) GatewayConstants.SUCCESS);
				Info info = new Info();
				info.setStatus(status);
				response.getOutputStream().print(gson.toJson(info));
				return;
			} catch (GatewayException e) {
				Info info = new Info();
				Status _st = new Status();
				_st.setCode((short) GatewayConstants.GENERAL_ERROR);
				_st.setMessage(e.getMessage());
				info.setStatus(_st);
				Info.Detail detail = new Info.Detail();
				info.setDetail(detail);
				response.getOutputStream().print(gson.toJson(info));
				return;
			} catch (Exception e) {
				Info info = new Info();
				Status _st = new Status();
				_st.setCode((short) GatewayConstants.GENERAL_ERROR);
				_st.setMessage(e.getMessage());
				info.setStatus(_st);
				Info.Detail detail = new Info.Detail();
				info.setDetail(detail);
				response.getOutputStream().print(gson.toJson(info));
				return;
			}
		} else {
			Detail detail = new Detail();
			Info info = new Info();
			Status status = new Status();
			status.setCode((short) GatewayConstants.GENERAL_ERROR);
			status.setMessage("User not logged");
			info.setStatus(status);
			info.setDetail(detail);
			response.getOutputStream().print(gson.toJson(info));
			return;

		}

	}

}
