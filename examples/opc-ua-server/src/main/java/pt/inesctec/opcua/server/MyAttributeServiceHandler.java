package pt.inesctec.opcua.server;

import java.util.Map;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.core.AttributeServiceSetHandler;
import org.opcfoundation.ua.core.Attributes;
import org.opcfoundation.ua.core.HistoryReadRequest;
import org.opcfoundation.ua.core.HistoryReadResponse;
import org.opcfoundation.ua.core.HistoryUpdateRequest;
import org.opcfoundation.ua.core.HistoryUpdateResponse;
import org.opcfoundation.ua.core.ReadRequest;
import org.opcfoundation.ua.core.ReadResponse;
import org.opcfoundation.ua.core.ReadValueId;
import org.opcfoundation.ua.core.ResponseHeader;
import org.opcfoundation.ua.core.ServiceFault;
import org.opcfoundation.ua.core.StatusCodes;
import org.opcfoundation.ua.core.WriteRequest;
import org.opcfoundation.ua.core.WriteResponse;
import org.opcfoundation.ua.core.WriteValue;
import org.opcfoundation.ua.transport.endpoint.EndpointServiceRequest;

public class MyAttributeServiceHandler implements AttributeServiceSetHandler {

	private NanoServer nanoServer;

	public MyAttributeServiceHandler(NanoServer nanoServer) {
		this.nanoServer = nanoServer;
	}

	@Override
	public void onHistoryRead(EndpointServiceRequest<HistoryReadRequest, HistoryReadResponse> req) throws ServiceFaultException {
		throw new ServiceFaultException(ServiceFault.createServiceFault(StatusCodes.Bad_NotImplemented));
	}

	@Override
	public void onHistoryUpdate(EndpointServiceRequest<HistoryUpdateRequest, HistoryUpdateResponse> req) throws ServiceFaultException {
		throw new ServiceFaultException(ServiceFault.createServiceFault(StatusCodes.Bad_NotImplemented));
	}

	/**
	 * Handle read request.
	 */
	@Override
	public void onRead(EndpointServiceRequest<ReadRequest, ReadResponse> req) throws ServiceFaultException {
		ReadRequest request = req.getRequest();
		ReadValueId[] nodesToRead = request.getNodesToRead();

		DataValue[] results = null;
		ReadResponse response = null;
		ResponseHeader h = Utils.checkRequestHeader(nanoServer, request.getRequestHeader());
		if (h.getServiceResult().isGood()) {

			if (request.getMaxAge() < 0) {
				h = new ResponseHeader(DateTime.currentTime(), request.getRequestHeader().getRequestHandle(), new StatusCode(StatusCodes.Bad_MaxAgeInvalid), null, null, null);
			}
			else if (request.getTimestampsToReturn() == null) {
				h = new ResponseHeader(DateTime.currentTime(), request.getRequestHeader().getRequestHandle(), new StatusCode(StatusCodes.Bad_TimestampsToReturnInvalid), null, null, null);
			}
			else if (nodesToRead != null) {
				// Do actual handling of NodesToRead

				results = new DataValue[nodesToRead.length];
				DateTime serverTimestamp = DateTime.currentTime();
				for (int i = 0; i < nodesToRead.length; i++) {
					results[i] = null;
					Map<UnsignedInteger, DataValue> attributeMap = nanoServer.onReadResultsMap.get(nodesToRead[i].getNodeId());

					if (attributeMap != null) {

						if (attributeMap.containsKey(nodesToRead[i].getAttributeId())) {
							results[i] = (DataValue) attributeMap.get(nodesToRead[i].getAttributeId()).clone();

							if (results[i] == null) {
								results[i] = new DataValue(new StatusCode(StatusCodes.Bad_AttributeIdInvalid));
							}
							else if (new UnsignedInteger(13).equals(nodesToRead[i].getAttributeId())) {
								// check maxAge
								DateTime currentTimestamp = results[i].getServerTimestamp();
								DateTime currentTime = DateTime.fromMillis(System.currentTimeMillis());
								long age = currentTime.getTimeInMillis() - currentTimestamp.getTimeInMillis();
								long maxAge = request.getMaxAge().longValue();
								long diff = maxAge - age;
								// If the server does not have a value that
								// is within the maximum age, it shall
								// attempt to read a new value from the data
								// source.
								// If maxAge is set to 0, the server shall
								// attempt to read a new value from the data
								// source.
								if (diff <= 0) {
									// read new value, simulated here by
									// refreshing timestamp
									results[i].setServerTimestamp(serverTimestamp);
								}
								/*
								 * This could also be checked here but it is now ignored: If maxAge is set to the
								 * max Int32 value or greater, the server shall attempt to get a cached value.
								 */

								if (request.getTimestampsToReturn() != null) {
									// check TimestampsToReturn
									switch (request.getTimestampsToReturn()) {
										case Source:
											results[i].setSourceTimestamp(serverTimestamp);
											results[i].setServerTimestamp(null);
											break;
										case Both:
											results[i].setSourceTimestamp(serverTimestamp);
											break;
										case Neither:
											results[i].setServerTimestamp(null);
											break;
										default:
											// case Server
											break;
									}
								}

							}
						}
						else {
							results[i] = new DataValue(new StatusCode(StatusCodes.Bad_AttributeIdInvalid));
						}
					}
					else {
						results[i] = new DataValue(new StatusCode(StatusCodes.Bad_NodeIdUnknown));
					}
				}
			}
			else {
				// NodesToRead is empty
				h = new ResponseHeader(DateTime.currentTime(), request.getRequestHeader().getRequestHandle(), new StatusCode(StatusCodes.Bad_NothingToDo), null, null, null);
			}
		}
		response = new ReadResponse(null, results, null);

		response.setResponseHeader(h);
		req.sendResponse(response);
	}

	/**
	 * Handle write request.
	 */
	@Override
	public void onWrite(EndpointServiceRequest<WriteRequest, WriteResponse> req) throws ServiceFaultException {

		WriteRequest request = req.getRequest();
		WriteValue[] nodesToWrite = request.getNodesToWrite();
		StatusCode[] results = null;
		StatusCode serviceResultCode = null;

		if (nodesToWrite != null) {
			// check here that Bad_TooManyOperations should not be set. No
			// limit for operations in this implementation.
			// Now set service result to GOOD always if nodesToWrite is not
			// null.
			serviceResultCode = StatusCode.GOOD;

			results = new StatusCode[nodesToWrite.length];
			for (int i = 0; i < nodesToWrite.length; i++) {
				// Get all attributes of the specified node
				Map<UnsignedInteger, DataValue> attributeMap = nanoServer.onReadResultsMap.get(nodesToWrite[i].getNodeId());

				if (attributeMap != null) {
					if (attributeMap.containsKey(nodesToWrite[i].getAttributeId())) {

						if (new UnsignedInteger(13).equals(nodesToWrite[i].getAttributeId())) {
							// Write value attribute
							// Check data type using nodes DataType
							// attribute
							// Validation is done with datatypeMap to enable
							// easy modification of valid data types
							NodeId datatype = (NodeId) attributeMap.get(Attributes.DataType).getValue().getValue();
							if (datatype == null) {
								// Error: Current node does not have data
								// type specified
								results[i] = new StatusCode(StatusCodes.Bad_TypeMismatch);
							}
							else {
								// Data type is defined for current node
								// Get java class corresponding to this OPC
								// UA data type
								Class<?> targetDataType = nanoServer.datatypeMap.get(datatype);
								if (targetDataType == null) {
									// No java data type found for this ua
									// type
									results[i] = new StatusCode(StatusCodes.Bad_TypeMismatch);
								}
								else {
									// Compare data type of value attribute
									// and value from write request
									if (targetDataType.isAssignableFrom(nodesToWrite[i].getValue().getValue().getValue().getClass())) {
										attributeMap.get(nodesToWrite[i].getAttributeId()).setValue(nodesToWrite[i].getValue().getValue());
										results[i] = StatusCode.GOOD;
									}
									else {
										// values do not match
										results[i] = new StatusCode(StatusCodes.Bad_TypeMismatch);
									}
								}
							}
						}
						else {
							// Write no other attribute than value.
							// Correct data type should also be checked
							// here.
							attributeMap.get(nodesToWrite[i].getAttributeId()).setValue(nodesToWrite[i].getValue().getValue());
						}
					}
					else {
						results[i] = new StatusCode(StatusCodes.Bad_AttributeIdInvalid);
					}
				}
				else {
					results[i] = new StatusCode(StatusCodes.Bad_NodeIdInvalid);
				}
			}
		}
		else {
			// Empty nodesToWrite array
			serviceResultCode = new StatusCode(StatusCodes.Bad_NothingToDo);
		}
		WriteResponse response = new WriteResponse(null, results, null);
		// Set response header to pass ctt check_responseHeader_error.js
		ResponseHeader h = new ResponseHeader(DateTime.currentTime(), request.getRequestHeader().getRequestHandle(), serviceResultCode, null, null, null);
		response.setResponseHeader(h);

		req.sendResponse(response);
	}

}
