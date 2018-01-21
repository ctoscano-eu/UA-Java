package pt.inesctec.opcua.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.opcfoundation.ua.builtintypes.ByteString;
import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.DiagnosticInfo;
import org.opcfoundation.ua.builtintypes.ExpandedNodeId;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.core.AddNodesRequest;
import org.opcfoundation.ua.core.AddNodesResponse;
import org.opcfoundation.ua.core.AddReferencesRequest;
import org.opcfoundation.ua.core.AddReferencesResponse;
import org.opcfoundation.ua.core.BrowseDescription;
import org.opcfoundation.ua.core.BrowseDirection;
import org.opcfoundation.ua.core.BrowseNextRequest;
import org.opcfoundation.ua.core.BrowseNextResponse;
import org.opcfoundation.ua.core.BrowsePath;
import org.opcfoundation.ua.core.BrowsePathResult;
import org.opcfoundation.ua.core.BrowsePathTarget;
import org.opcfoundation.ua.core.BrowseRequest;
import org.opcfoundation.ua.core.BrowseResponse;
import org.opcfoundation.ua.core.BrowseResult;
import org.opcfoundation.ua.core.DeleteNodesRequest;
import org.opcfoundation.ua.core.DeleteNodesResponse;
import org.opcfoundation.ua.core.DeleteReferencesRequest;
import org.opcfoundation.ua.core.DeleteReferencesResponse;
import org.opcfoundation.ua.core.NodeManagementServiceSetHandler;
import org.opcfoundation.ua.core.QueryFirstRequest;
import org.opcfoundation.ua.core.QueryFirstResponse;
import org.opcfoundation.ua.core.QueryNextRequest;
import org.opcfoundation.ua.core.QueryNextResponse;
import org.opcfoundation.ua.core.ReferenceDescription;
import org.opcfoundation.ua.core.RegisterNodesRequest;
import org.opcfoundation.ua.core.RegisterNodesResponse;
import org.opcfoundation.ua.core.RelativePath;
import org.opcfoundation.ua.core.RelativePathElement;
import org.opcfoundation.ua.core.RequestHeader;
import org.opcfoundation.ua.core.ResponseHeader;
import org.opcfoundation.ua.core.ServiceFault;
import org.opcfoundation.ua.core.StatusCodes;
import org.opcfoundation.ua.core.TranslateBrowsePathsToNodeIdsRequest;
import org.opcfoundation.ua.core.TranslateBrowsePathsToNodeIdsResponse;
import org.opcfoundation.ua.core.UnregisterNodesRequest;
import org.opcfoundation.ua.core.UnregisterNodesResponse;
import org.opcfoundation.ua.transport.endpoint.EndpointServiceRequest;

public class MyNodeManagementServiceHandler implements NodeManagementServiceSetHandler {

	private NanoServer nanoServer;

	public MyNodeManagementServiceHandler(NanoServer nanoServer) {
		this.nanoServer = nanoServer;
	}

	@Override
	public void onAddNodes(EndpointServiceRequest<AddNodesRequest, AddNodesResponse> req) throws ServiceFaultException {
		throw new ServiceFaultException(ServiceFault.createServiceFault(StatusCodes.Bad_NotImplemented));
	}

	@Override
	public void onAddReferences(EndpointServiceRequest<AddReferencesRequest, AddReferencesResponse> req) throws ServiceFaultException {
		throw new ServiceFaultException(ServiceFault.createServiceFault(StatusCodes.Bad_NotImplemented));
	}

	@Override
	public void onBrowse(EndpointServiceRequest<BrowseRequest, BrowseResponse> req) throws ServiceFaultException {
		BrowseRequest request = req.getRequest();

		List<BrowseResult> browseResults = new ArrayList<BrowseResult>();

		ResponseHeader h = Utils.checkRequestHeader(nanoServer, request.getRequestHeader());

		// If ViewId is not null, Bad_ViewIdUnknown is always returned.
		if (request.getView() != null) {
			if (!NodeId.isNull(request.getView().getViewId())) {
				h = new ResponseHeader(DateTime.currentTime(), request.getRequestHeader().getRequestHandle(), new StatusCode(StatusCodes.Bad_ViewIdUnknown), null, null, null);
			}
		}

		BrowseDescription[] nodesToBrowse = request.getNodesToBrowse();
		if (nodesToBrowse != null) {

			for (int i = 0; i < nodesToBrowse.length; i++) {
				BrowseResult nextBrowseResult = null;

				BrowseDescription currentNodeToBrowse = nodesToBrowse[i];
				NodeId nodeId = currentNodeToBrowse.getNodeId();
				if (nanoServer.onBrowseActions.containsKey(nodeId)) {
					nextBrowseResult = nanoServer.onBrowseActions.get(nodeId).clone();
					/*
					 * Error handling
					 */
				}
				else {
					// If NodeId is null, consider the syntax of the node id
					// not valid.
					if (NodeId.isNull(nodeId)) {
						browseResults.add(new BrowseResult(new StatusCode(StatusCodes.Bad_NodeIdInvalid), null, null));
					}
					else {
						// If NodeId is not found from browse actions
						// hashmap and is not null, then consider the NodeId
						// unknown
						browseResults.add(new BrowseResult(new StatusCode(StatusCodes.Bad_NodeIdUnknown), null, null));
					}
					continue;
				}
				NodeId referenceTypeId = currentNodeToBrowse.getReferenceTypeId();
				if ((!NodeId.isNull(referenceTypeId) && !nanoServer.onReadResultsMap.containsKey(referenceTypeId)) || (!NodeId.isNull(referenceTypeId) && !Utils.isReferenceType(referenceTypeId))) {
					// We need to somehow decide whether the ReferenceTypeId
					// is invalid. If NodeId is not found from
					// onReadResultsMap, then the ReferenceType is not
					// valid.
					browseResults.add(new BrowseResult(new StatusCode(StatusCodes.Bad_ReferenceTypeIdInvalid), null, null));
					continue;
				}
				BrowseDirection browseDirection = currentNodeToBrowse.getBrowseDirection();
				if (browseDirection == null || (!browseDirection.equals(BrowseDirection.Both) && !browseDirection.equals(BrowseDirection.Forward) && !browseDirection.equals(BrowseDirection.Inverse))) {
					browseResults.add(new BrowseResult(new StatusCode(StatusCodes.Bad_BrowseDirectionInvalid), null, null));
					continue;
				}

				// getBrowsePathTarget method contains code that is shared
				// by onBrowse- and onTranslateBrowsePathsToNodeIds methods.
				nextBrowseResult = Utils.getBrowsePathTarget(nanoServer, nodeId, referenceTypeId, browseDirection, currentNodeToBrowse.getIncludeSubtypes());

				/*
				 * Responses are "filtered" by deleting unwanted references.
				 */
				UnsignedInteger nodeClassMask = currentNodeToBrowse.getNodeClassMask();
				if (!(new UnsignedInteger(255).equals(nodeClassMask)) && !(new UnsignedInteger(0).equals(nodeClassMask))) {
					// Node Class Mask is not 255 or 0.
					/*
					 * return references matching the node class as specified by the NodeClassMask Bit
					 * NodeClass 0 Object 1 Variable 2 Method 3 ObjectType 4 VariableType 5 ReferenceType 6
					 * DataType 7 View
					 */
					ReferenceDescription[] referenceDescriptions = nextBrowseResult.getReferences();
					List<ReferenceDescription> newReferenceDescriptions = new ArrayList<ReferenceDescription>();

					if (referenceDescriptions != null) {
						for (int j = 0; j < referenceDescriptions.length; j++) {
							ReferenceDescription rd = referenceDescriptions[j];
							if ((rd.getNodeClass().getValue() & nodeClassMask.getValue()) != 0) {
								newReferenceDescriptions.add(rd);
							}
						}
						nextBrowseResult.setReferences(newReferenceDescriptions.toArray(new ReferenceDescription[newReferenceDescriptions.size()]));
					}
				}
				if (currentNodeToBrowse.getResultMask().intValue() != 63) {
					// Specifies the fields in the ReferenceDescription
					// structure that should be returned.
					// OPC UA part 4 - Services page 40, table 30.
					ReferenceDescription[] referenceDescriptions = nextBrowseResult.getReferences();
					if (referenceDescriptions != null) {
						for (ReferenceDescription rd : referenceDescriptions) {
							if ((1 & currentNodeToBrowse.getResultMask().intValue()) == 0) {
								// ReferenceType to null
								rd.setReferenceTypeId(null);
							}
							if ((2 & currentNodeToBrowse.getResultMask().intValue()) == 0) {
								// IsForward to null
								rd.setIsForward(null);
							}
							if ((4 & currentNodeToBrowse.getResultMask().intValue()) == 0) {
								// NodeClass to null
								rd.setNodeClass(null);
							}
							if ((8 & currentNodeToBrowse.getResultMask().intValue()) == 0) {
								// BrowseName to null
								rd.setBrowseName(null);
							}
							if ((16 & currentNodeToBrowse.getResultMask().intValue()) == 0) {
								// DisplayName to null
								rd.setDisplayName(null);
							}
							if ((32 & currentNodeToBrowse.getResultMask().intValue()) == 0) {
								// TypeDefinition to null
								rd.setTypeDefinition(null);
							}
						}
					}
				}
				if ((request.getRequestedMaxReferencesPerNode() != null) && (request.getRequestedMaxReferencesPerNode().intValue() != 0)) {

					if (nextBrowseResult.getReferences() != null) {

						int maxReferencesPerNode = request.getRequestedMaxReferencesPerNode().intValue();
						// Amount of results needs to be limited only if
						// RequestedMaxReferencesPerNode is smaller than
						// amount of references
						if (nextBrowseResult.getReferences().length > maxReferencesPerNode) {
							if (nanoServer.continuationPoint == null) {
								ReferenceDescription[] referenceDescriptions = nextBrowseResult.getReferences();
								List<ReferenceDescription> newReferenceDescriptions = new ArrayList<ReferenceDescription>();
								List<ReferenceDescription> continuationPointReferenceDescriptions = new ArrayList<ReferenceDescription>();

								for (int j = 0; j < maxReferencesPerNode; j++) {
									newReferenceDescriptions.add(referenceDescriptions[j]);
								}
								for (int j = maxReferencesPerNode; j < referenceDescriptions.length; j++) {
									continuationPointReferenceDescriptions.add(referenceDescriptions[j]);
								}
								nextBrowseResult.setReferences(newReferenceDescriptions.toArray(new ReferenceDescription[newReferenceDescriptions.size()]));

								ByteString continuationPointIdentifier = ByteString.valueOf((byte) 1);
								nextBrowseResult.setContinuationPoint(continuationPointIdentifier);
								nanoServer.continuationPoint = new ContinuationPoint(nanoServer, request.getRequestedMaxReferencesPerNode(),
								    continuationPointReferenceDescriptions.toArray(new ReferenceDescription[continuationPointReferenceDescriptions.size()]), request.getRequestHeader().getAuthenticationToken(),
								    continuationPointIdentifier);
							}
							else {
								// There's only one continuation point
								// available in this server. If it is not
								// null, then no other continuation points
								// are available.
								nextBrowseResult = new BrowseResult(new StatusCode(StatusCodes.Bad_NoContinuationPoints), null, null);
							}
						}
					}
				}
				browseResults.add(nextBrowseResult);
			}
			// No errors to report in response header
			if (h == null) {
				h = new ResponseHeader(DateTime.currentTime(), request.getRequestHeader().getRequestHandle(), StatusCode.GOOD, null, null, null);
			}
		}
		else {
			// Bad_NothingToDo There was nothing to do because the client
			// passed a list of operations with no elements.
			h = new ResponseHeader(DateTime.currentTime(), request.getRequestHeader().getRequestHandle(), new StatusCode(StatusCodes.Bad_NothingToDo), null, null, null);
		}

		BrowseResponse response = new BrowseResponse(null, browseResults.toArray(new BrowseResult[browseResults.size()]), null);
		response.setResponseHeader(h);

		req.sendResponse(response);

	}

	@Override
	public void onBrowseNext(EndpointServiceRequest<BrowseNextRequest, BrowseNextResponse> req) throws ServiceFaultException {

		BrowseNextRequest request = req.getRequest();
		RequestHeader requestHeader = request.getRequestHeader();
		ByteString[] continuationPoints = request.getContinuationPoints();

		BrowseResult[] browseResults = new BrowseResult[continuationPoints.length];
		DiagnosticInfo[] diagnosticInfoResults = null;
		boolean continuationPointToNull = false;
		ResponseHeader h = Utils.checkRequestHeader(nanoServer, requestHeader);

		UnsignedInteger returnDiagnostics = requestHeader.getReturnDiagnostics();
		if (continuationPoints == null || continuationPoints.length == 0) {

			h = new ResponseHeader(DateTime.currentTime(), requestHeader.getRequestHandle(), new StatusCode(StatusCodes.Bad_NothingToDo), null, null, null);
			if (!returnDiagnostics.equals(new UnsignedInteger(0)) && returnDiagnostics != null) {
				diagnosticInfoResults = new DiagnosticInfo[1];
				diagnosticInfoResults[0] = new DiagnosticInfo("Some diagnostics.", null, null, null, null, 0, null);
				h.setStringTable(new String[] { "Some diagnostics available." });
			}
		}
		else if (nanoServer.continuationPoint != null && !request.getReleaseContinuationPoints()) {
			// Even though this implementation supports only one
			// continuation point, this structure should be expandable
			if (!returnDiagnostics.equals(new UnsignedInteger(0)) && returnDiagnostics != null) {
				diagnosticInfoResults = new DiagnosticInfo[continuationPoints.length];
			}
			int pos = 0;
			for (ByteString continuationPointRequested : continuationPoints) {
				if (!continuationPointRequested.equals(nanoServer.continuationPoint.getCurrentContinuationPoint())
				    || !requestHeader.getAuthenticationToken().equals(nanoServer.continuationPoint.getAuthenticationToken())) {
					// Request contained old Continuation Point or
					// continuation point was not affiliated with this
					// session.
					browseResults[pos] = new BrowseResult(new StatusCode(StatusCodes.Bad_ContinuationPointInvalid), null, null);
				}
				else {
					// Actually get the next set of results
					browseResults[pos] = nanoServer.continuationPoint.getNextReferencesDescriptions(continuationPointRequested);
					if (browseResults[pos].getContinuationPoint() == null) {
						continuationPointToNull = true;
					}
				}
				if (!returnDiagnostics.equals(new UnsignedInteger(0)) && returnDiagnostics != null) {
					diagnosticInfoResults[pos] = new DiagnosticInfo("Some diagnostics.", null, null, null, null, 0, null);
					h.setStringTable(new String[] { "" });
				}
				pos++;
			}
		}
		else if (nanoServer.continuationPoint == null) {
			// Client called browse next service even though no
			// continuationPoint exists.
			if ((returnDiagnostics != null) && (returnDiagnostics.intValue() != 0)) {
				diagnosticInfoResults = new DiagnosticInfo[continuationPoints.length];
			}

			for (int pos = 0; pos < continuationPoints.length; pos++) {
				browseResults[pos] = new BrowseResult(new StatusCode(StatusCodes.Bad_ContinuationPointInvalid), null, null);
				if ((returnDiagnostics != null) && (returnDiagnostics.intValue() != 0)) {
					diagnosticInfoResults[pos] = new DiagnosticInfo("No continuationPoint exists.", null, null, null, null, 0, null);
					h.setStringTable(new String[] { "No continuationPoint exists." });
				}
			}
		}
		if ((nanoServer.continuationPoint != null && request.getReleaseContinuationPoints()) || continuationPointToNull) {
			nanoServer.continuationPoint = null;
		}
		if ((returnDiagnostics != null) && (returnDiagnostics.intValue() != 0)) {
			h.setServiceDiagnostics(new DiagnosticInfo());
		}
		BrowseNextResponse browseNextResponse = new BrowseNextResponse(h, browseResults, diagnosticInfoResults);

		req.sendResponse(browseNextResponse);

	}

	@Override
	public void onDeleteNodes(EndpointServiceRequest<DeleteNodesRequest, DeleteNodesResponse> req) throws ServiceFaultException {
		throw new ServiceFaultException(ServiceFault.createServiceFault(StatusCodes.Bad_NotImplemented));
	}

	@Override
	public void onDeleteReferences(EndpointServiceRequest<DeleteReferencesRequest, DeleteReferencesResponse> req) throws ServiceFaultException {
		throw new ServiceFaultException(ServiceFault.createServiceFault(StatusCodes.Bad_NotImplemented));
	}

	@Override
	public void onQueryFirst(EndpointServiceRequest<QueryFirstRequest, QueryFirstResponse> req) throws ServiceFaultException {
		throw new ServiceFaultException(ServiceFault.createServiceFault(StatusCodes.Bad_NotImplemented));
	}

	@Override
	public void onQueryNext(EndpointServiceRequest<QueryNextRequest, QueryNextResponse> req) throws ServiceFaultException {
		throw new ServiceFaultException(ServiceFault.createServiceFault(StatusCodes.Bad_NotImplemented));
	}

	@Override
	public void onRegisterNodes(EndpointServiceRequest<RegisterNodesRequest, RegisterNodesResponse> req) throws ServiceFaultException {

		RegisterNodesRequest request = req.getRequest();

		NodeId[] registeredNodeIds = null;
		ResponseHeader h = Utils.checkRequestHeader(nanoServer, request.getRequestHeader());
		if (request.getNodesToRegister() == null || request.getNodesToRegister().length == 0) {
			// Bad_NothingToDo
			h.setServiceResult(new StatusCode(StatusCodes.Bad_NothingToDo));
			UnsignedInteger returnDiagnostics = request.getRequestHeader().getReturnDiagnostics();
			if ((returnDiagnostics != null) && (returnDiagnostics.intValue() != 0)) {
				// return service diagnostics
				h.setServiceDiagnostics(new DiagnosticInfo("Some diagnostics: Bad_NothingToDo.", null, null, null, null, 0, null));
				h.setStringTable(new String[] { "Some diagnostics: Bad_NothingToDo." });

			}
		}
		else {
			registeredNodeIds = new NodeId[request.getNodesToRegister().length];
			int pos = 0;
			for (NodeId nodeId : request.getNodesToRegister()) {
				// Do some custom checking on individual nodes, now only
				// pass requested nodes to response
				registeredNodeIds[pos] = nodeId;
				pos++;
			}
		}

		RegisterNodesResponse registerNodesResponse = new RegisterNodesResponse(h, registeredNodeIds);

		req.sendResponse(registerNodesResponse);
	}

	@Override
	public void onTranslateBrowsePathsToNodeIds(EndpointServiceRequest<TranslateBrowsePathsToNodeIdsRequest, TranslateBrowsePathsToNodeIdsResponse> req) throws ServiceFaultException {
		TranslateBrowsePathsToNodeIdsRequest request = req.getRequest();

		BrowsePathResult[] browsePathResults = null;
		DiagnosticInfo[] diagnosticInfos = null;
		// Check request header contents and return response header
		// accordingly.
		ResponseHeader h = Utils.checkRequestHeader(nanoServer, request.getRequestHeader());

		UnsignedInteger requestedReturnDiagnostics = request.getRequestHeader().getReturnDiagnostics();
		if (request.getBrowsePaths() == null || request.getBrowsePaths().length == 0) {
			// Bad_NothingToDo
			h.setServiceResult(new StatusCode(StatusCodes.Bad_NothingToDo));
			if (requestedReturnDiagnostics != null && !requestedReturnDiagnostics.equals(new UnsignedInteger(0))) {
				// return service diagnostics
				h.setServiceDiagnostics(new DiagnosticInfo("Diagnostics: Bad_NothingToDo.", null, null, null, null, 0, null));
				h.setStringTable(new String[] { "Diagnostics: Bad_NothingToDo." });

			}
		}
		else {
			// There is at least one browse path to browse
			int browsePathLength = request.getBrowsePaths().length;
			if ((requestedReturnDiagnostics != null) && (requestedReturnDiagnostics.intValue() != 0)) {
				diagnosticInfos = new DiagnosticInfo[browsePathLength]; // initialize
			}
			// diagnostics
			// if
			// they
			// are
			// requested.
			browsePathResults = new BrowsePathResult[browsePathLength];

			for (int i = 0; i < browsePathLength; i++) {
				BrowsePath bp = request.getBrowsePaths()[i];
				NodeId startingNode = bp.getStartingNode();

				// test is requested node is null
				if (NodeId.isNull(startingNode)) {
					browsePathResults[i] = new BrowsePathResult(new StatusCode(StatusCodes.Bad_NodeIdInvalid), null);
					// return diagnostics in the case of missing starting
					// node
					if ((requestedReturnDiagnostics != null) && (requestedReturnDiagnostics.intValue() != 0)) {
						// return service diagnostics
						h.setServiceDiagnostics(new DiagnosticInfo("Diagnostics: Bad_NodeIdInvalid", null, null, null, null, 0, null));
						h.setStringTable(new String[] { "Diagnostics: Bad_NodeIdInvalid" });
						diagnosticInfos[i] = new DiagnosticInfo("DiagnosticInfo", null, null, null, 0, null, null);
					}
					continue;
				}
				// if node is not null, test if node is found on server at
				// all
				Map<UnsignedInteger, DataValue> attributeMap = nanoServer.onReadResultsMap.get(startingNode);
				if (attributeMap == null) {
					// Not found in read results
					browsePathResults[i] = new BrowsePathResult(new StatusCode(StatusCodes.Bad_NodeIdUnknown), null);
					// return diagnostics in the case of unknown starting
					// node
					if ((requestedReturnDiagnostics != null) && (requestedReturnDiagnostics.intValue() != 0)) {
						// return service diagnostics
						h.setServiceDiagnostics(new DiagnosticInfo("Diagnostics: Bad_NodeIdUnknown", null, null, null, null, 0, null));
						h.setStringTable(new String[] { "Diagnostics: Bad_NodeIdUnknown" });
						diagnosticInfos[i] = new DiagnosticInfo("DiagnosticInfo", null, null, null, 0, null, null);
					}
					continue;
				}
				// test if client requested empty relative path
				RelativePath relativePath = bp.getRelativePath();
				if (relativePath == null || relativePath.getElements() == null || relativePath.getElements().length == 0) {
					// if relative path is empty then return Bad_NothingToDo
					browsePathResults[i] = new BrowsePathResult(new StatusCode(StatusCodes.Bad_NothingToDo), null);
					if ((requestedReturnDiagnostics != null) && (requestedReturnDiagnostics.intValue() != 0)) {
						// return service diagnostics
						h.setServiceDiagnostics(new DiagnosticInfo("Diagnostics: Bad_NothingToDo (relative path was empty).", null, null, null, null, 0, null));
						h.setStringTable(new String[] { "Diagnostics: Bad_NothingToDo (relative path was empty)." });
						diagnosticInfos[i] = new DiagnosticInfo("DiagnosticInfo", null, null, null, 0, null, null);
					}
					continue;
				}

				RelativePathElement[] relativePathElements = relativePath.getElements();
				int relativePathElementsLength = relativePathElements.length;
				RelativePathElement lastElement = relativePathElements[relativePathElementsLength - 1];
				// Specification part 4 page 44: The last element in the
				// relativePath shall always have a targetName specified.
				if (QualifiedName.isNull(lastElement.getTargetName())) {
					browsePathResults[i] = new BrowsePathResult(new StatusCode(StatusCodes.Bad_BrowseNameInvalid), null);
					continue;
				}

				// After testing that values are valid, go through relative
				// path elements of current browse path and return correct
				// BrowsePathTarget.
				// In some cases also multiple valid BrowsePathTargets may
				// be found.
				ArrayList<BrowsePathTarget> targets = new ArrayList<BrowsePathTarget>();

				for (int j = 0; j < relativePathElementsLength; j++) {
					RelativePathElement rpe = relativePathElements[j];
					// Test if target name (browse name) is null. In that
					// case return Bad_BrowseNameInvalid. However the final
					// element may have an empty target name.
					if (QualifiedName.isNull(rpe.getTargetName()) && j < (relativePathElementsLength - 1)) {
						browsePathResults[i] = new BrowsePathResult(new StatusCode(StatusCodes.Bad_BrowseNameInvalid), null);
						break;
					}
					// relative path element defines the browse direction
					BrowseDirection browseDirection = BrowseDirection.Forward;
					if (rpe.getIsInverse()) {
						browseDirection = BrowseDirection.Inverse;
					}
					// Return all browse results from starting node with
					// certain reference type id and browse direction
					BrowseResult results = Utils.getBrowsePathTarget(nanoServer, startingNode, rpe.getReferenceTypeId(), browseDirection, rpe.getIncludeSubtypes());

					// Look through the browse results to find one with
					// correct target name
					ExpandedNodeId match = null;
					if (results.getReferences() != null) {
						for (ReferenceDescription rd : results.getReferences()) {
							QualifiedName browseName = rd.getBrowseName();
							if (browseName.equals(rpe.getTargetName())) {
								// Found match with target name, this is
								// starting node for next iteration
								// Convert from expanded node id to node id:
								Object value = rd.getNodeId().getValue();
								int namespaceIndex = rd.getNodeId().getNamespaceIndex();
								switch (rd.getNodeId().getIdType()) {
									case Numeric:
										startingNode = new NodeId(namespaceIndex, (UnsignedInteger) value);
										break;
									case String:
										startingNode = new NodeId(namespaceIndex, (String) value);
										break;
									case Guid:
										startingNode = new NodeId(namespaceIndex, (UUID) value);
										break;
									case Opaque:
										startingNode = new NodeId(namespaceIndex, (byte[]) value);
										break;
								}

								if (browseName.equals(lastElement.getTargetName())) {
									// Return last target of multiple
									// RelativePathElements
									match = rd.getNodeId();
									// Specification part 4, page 43: If a Node
									// has multiple targets with the same
									// BrowseName, the Server shall return a
									// list of NodeIds
									targets.add(new BrowsePathTarget(match, UnsignedInteger.MAX_VALUE));
								}
							}
						}
					}
				}
				// Check whether valid targets have been found
				if (targets.size() == 0 && browsePathResults[i] == null) {
					// if not then return status code Bad_NoMatch
					browsePathResults[i] = new BrowsePathResult(new StatusCode(StatusCodes.Bad_NoMatch), null);
				}
				else if (targets.size() > 0 && browsePathResults[i] == null) {
					// otherwise everything is correct, return status code
					// GOOD
					browsePathResults[i] = new BrowsePathResult(StatusCode.GOOD, targets.toArray(new BrowsePathTarget[targets.size()]));
				}
				if ((requestedReturnDiagnostics != null) && (requestedReturnDiagnostics.intValue() != 0)) {
					// return service diagnostics
					h.setServiceDiagnostics(new DiagnosticInfo("Diagnostics: everything normal.", null, null, null, null, 0, null));
					h.setStringTable(new String[] { "Diagnostics: everything normal." });
					diagnosticInfos[i] = new DiagnosticInfo("DiagnosticInfo", null, null, null, 0, null, null);
				}
			}
		}

		TranslateBrowsePathsToNodeIdsResponse translateBrowsePathsToNodeIdsResponse = new TranslateBrowsePathsToNodeIdsResponse(h, browsePathResults, diagnosticInfos);
		req.sendResponse(translateBrowsePathsToNodeIdsResponse);

	}

	@Override
	public void onUnregisterNodes(EndpointServiceRequest<UnregisterNodesRequest, UnregisterNodesResponse> req) throws ServiceFaultException {

		UnregisterNodesRequest request = req.getRequest();

		ResponseHeader h = Utils.checkRequestHeader(nanoServer, request.getRequestHeader());
		NodeId[] nodesToUnregister = request.getNodesToUnregister();
		if (nodesToUnregister == null || nodesToUnregister.length == 0) {
			// Bad_NothingToDo
			h.setServiceResult(new StatusCode(StatusCodes.Bad_NothingToDo));
			UnsignedInteger returnDiagnostics = request.getRequestHeader().getReturnDiagnostics();
			if ((returnDiagnostics != null) && (returnDiagnostics.intValue() != 0)) {
				// return service diagnostics
				h.setServiceDiagnostics(new DiagnosticInfo("Some diagnostics: Bad_NothingToDo.", null, null, null, null, 0, null));
				h.setStringTable(new String[] { "Some diagnostics: Bad_NothingToDo." });

			}
		} /*
		   * else { for(NodeId nodeId : request.getNodesToUnregister()) { ////Do some custom checking
		   * on individual nodes, now all nodes are unregistered equally (do nothing)
		   * 
		   * } }
		   */
		UnregisterNodesResponse unRegisterNodesResponse = new UnregisterNodesResponse(h);
		req.sendResponse(unRegisterNodesResponse);

	}

}