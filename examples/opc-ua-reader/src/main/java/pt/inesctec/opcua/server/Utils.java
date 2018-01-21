package pt.inesctec.opcua.server;

import java.util.ArrayList;
import java.util.List;

import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.core.BrowseDirection;
import org.opcfoundation.ua.core.BrowseResult;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.ReferenceDescription;
import org.opcfoundation.ua.core.RequestHeader;
import org.opcfoundation.ua.core.ResponseHeader;
import org.opcfoundation.ua.core.StatusCodes;

public class Utils {
	/**
	 * Check request header contents and return response header accordingly.
	 *
	 * @param requestHeader
	 *          the request header to check.
	 * @return ResponseHeader
	 *
	 */
	public static ResponseHeader checkRequestHeader(NanoServer nanoServer, RequestHeader requestHeader) {
		// set responseheader to StatusCode.GOOD by default.
		ResponseHeader h = new ResponseHeader(DateTime.currentTime(), requestHeader.getRequestHandle(), StatusCode.GOOD, null, null, null);

		if (NodeId.isNull(requestHeader.getAuthenticationToken()) || !nanoServer.validAuthenticationTokens.contains(requestHeader.getAuthenticationToken())) {
			// AuthenticationToken was null or invalid
			if (nanoServer.sessions.contains(requestHeader.getAuthenticationToken())) {
				// Session is created but not activated
				h = new ResponseHeader(DateTime.currentTime(), requestHeader.getRequestHandle(), new StatusCode(StatusCodes.Bad_SessionNotActivated), null, null, null);
				// This is an error condition: close this session
				nanoServer.sessions.remove(requestHeader.getAuthenticationToken());
			}
			else {
				h = new ResponseHeader(DateTime.currentTime(), requestHeader.getRequestHandle(), new StatusCode(StatusCodes.Bad_SessionIdInvalid), null, null, null);
			}

		}
		else if (requestHeader.getTimestamp().equals(new DateTime(0))) {
			// Timestamp is now only checked with value 0
			// TimeStamp was not valid
			h = new ResponseHeader(DateTime.currentTime(), requestHeader.getRequestHandle(), new StatusCode(StatusCodes.Bad_InvalidTimestamp), null, null, null);

		}
		return h;
	}

	/**
	 * Return node by browsing from starting node and ending in target name.
	 *
	 * @param startingNode
	 *          the request header to check.
	 * @param referenceTypeId
	 *          reference types to follow. See also includeSubtypes parameter.
	 * @param browseDirection
	 *          tells whether to check forward or inverse references.
	 * @param includeSubtypes
	 *          browse subtypes or not.
	 * @return BrowseResult
	 *
	 */
	public static BrowseResult getBrowsePathTarget(NanoServer nanoServer, NodeId startingNode, NodeId referenceTypeId, BrowseDirection browseDirection, Boolean includeSubtypes) {

		BrowseResult nextBrowseResult = nanoServer.onBrowseActions.get(startingNode).clone();

		if (browseDirection.equals(BrowseDirection.Inverse)) {
			// return only inverse references
			ReferenceDescription[] referenceDescriptions = nextBrowseResult.getReferences();
			List<ReferenceDescription> newReferenceDescriptions = new ArrayList<ReferenceDescription>();

			if (referenceDescriptions != null) {
				for (int j = 0; j < referenceDescriptions.length; j++) {
					if (!referenceDescriptions[j].getIsForward()) {
						newReferenceDescriptions.add(referenceDescriptions[j]);
					}
				}
				nextBrowseResult.setReferences(newReferenceDescriptions.toArray(new ReferenceDescription[newReferenceDescriptions.size()]));
			}
		}
		else if (browseDirection.equals(BrowseDirection.Forward)) {
			// return only forward references
			ReferenceDescription[] referenceDescriptions = nextBrowseResult.getReferences();
			List<ReferenceDescription> newReferenceDescriptions = new ArrayList<ReferenceDescription>();

			if (referenceDescriptions != null) {
				for (int j = 0; j < referenceDescriptions.length; j++) {
					if (referenceDescriptions[j].getIsForward()) {
						newReferenceDescriptions.add(referenceDescriptions[j]);
					}
				}
				nextBrowseResult.setReferences(newReferenceDescriptions.toArray(new ReferenceDescription[newReferenceDescriptions.size()]));
			}
		}
		// OPC UA standard part 3, page 56 illustrates hierarchy of reference
		// types
		// Current implementation only takes into account References,
		// HierarchicalReferences, NonHierarchicalReferences and HasChild. For
		// example HasEventSource -> HasNotifier relation won't work.
		// ReferenceTypeId=i=32 -> NonHierarchicalReferences
		if (Identifiers.NonHierarchicalReferences.equals(referenceTypeId)) {
			// NonHierarchicalReferences requested
			ReferenceDescription[] referenceDescriptions = nextBrowseResult.getReferences();
			List<ReferenceDescription> newReferenceDescriptions = new ArrayList<ReferenceDescription>();

			if (referenceDescriptions != null) {
				for (int j = 0; j < referenceDescriptions.length; j++) {
					ReferenceDescription rd = referenceDescriptions[j];
					if (includeSubtypes && (Identifiers.GeneratesEvent.equals(rd.getReferenceTypeId()) || Identifiers.AlwaysGeneratesEvent.equals(rd.getReferenceTypeId())
					    || Identifiers.HasEncoding.equals(rd.getReferenceTypeId()) || Identifiers.HasModellingRule.equals(rd.getReferenceTypeId()) || Identifiers.HasDescription.equals(rd.getReferenceTypeId())
					    || Identifiers.HasTypeDefinition.equals(rd.getReferenceTypeId()))) {
						newReferenceDescriptions.add(rd);
					}
					else if (!includeSubtypes && Identifiers.NonHierarchicalReferences.equals(rd.getReferenceTypeId())) {
						// return only references of type
						// NonHierarchicalReferences (i=32), do not include sub
						// types
						newReferenceDescriptions.add(rd);
					}
				}
				nextBrowseResult.setReferences(newReferenceDescriptions.toArray(new ReferenceDescription[newReferenceDescriptions.size()]));
			}
		}
		else if (Identifiers.HierarchicalReferences.equals(referenceTypeId)) {
			// ReferenceTypeId=i=33
			ReferenceDescription[] referenceDescriptions = nextBrowseResult.getReferences();
			List<ReferenceDescription> newReferenceDescriptions = new ArrayList<ReferenceDescription>();

			if (referenceDescriptions != null) {
				for (int j = 0; j < referenceDescriptions.length; j++) {
					ReferenceDescription rd = referenceDescriptions[j];
					if (includeSubtypes && (Identifiers.HasComponent.equals(rd.getReferenceTypeId()) || Identifiers.HasProperty.equals(rd.getReferenceTypeId())
					    || Identifiers.HasOrderedComponent.equals(rd.getReferenceTypeId()) || Identifiers.HasSubtype.equals(rd.getReferenceTypeId()) || Identifiers.Organizes.equals(rd.getReferenceTypeId())
					    || Identifiers.HasEventSource.equals(rd.getReferenceTypeId()) || Identifiers.HasNotifier.equals(rd.getReferenceTypeId()))) {
						newReferenceDescriptions.add(rd);
					}
					else if (!includeSubtypes && Identifiers.HierarchicalReferences.equals(rd.getReferenceTypeId())) {
						// return only references of type HierarchicalReferences
						// (i=33), specified here separately
						newReferenceDescriptions.add(rd);
					}
				}
				nextBrowseResult.setReferences(newReferenceDescriptions.toArray(new ReferenceDescription[newReferenceDescriptions.size()]));
			}
		}
		else if (Identifiers.HasChild.equals(referenceTypeId)) {
			// ReferenceTypeId=i=34
			ReferenceDescription[] referenceDescriptions = nextBrowseResult.getReferences();
			List<ReferenceDescription> newReferenceDescriptions = new ArrayList<ReferenceDescription>();

			if (referenceDescriptions != null) {
				for (int j = 0; j < referenceDescriptions.length; j++) {
					ReferenceDescription rd = referenceDescriptions[j];
					if (includeSubtypes && (Identifiers.HasComponent.equals(rd.getReferenceTypeId()) || Identifiers.HasProperty.equals(rd.getReferenceTypeId())
					    || Identifiers.HasOrderedComponent.equals(rd.getReferenceTypeId()) || Identifiers.HasSubtype.equals(rd.getReferenceTypeId()) || Identifiers.Aggregates.equals(rd.getReferenceTypeId()))) {
						newReferenceDescriptions.add(rd);
					}
					else if (!includeSubtypes && Identifiers.HasChild.equals(rd.getReferenceTypeId())) {
						// return only references of type HierarchicalReferences
						// (i=33), specified here separately
						newReferenceDescriptions.add(rd);
					}
				}
				nextBrowseResult.setReferences(newReferenceDescriptions.toArray(new ReferenceDescription[newReferenceDescriptions.size()]));
			}
			// ReferenceTypeId=i=31 means that all references should be
			// returned. If some other ReferenceTypeId is specified, then only
			// return that references with that ReferenceTypeId.
		}
		else if (!(referenceTypeId.equals(Identifiers.References)) && !(NodeId.isNull(referenceTypeId))) {
			ReferenceDescription[] referenceDescriptions = nextBrowseResult.getReferences();
			List<ReferenceDescription> newReferenceDescriptions = new ArrayList<ReferenceDescription>();

			if (referenceDescriptions != null) {
				for (int j = 0; j < referenceDescriptions.length; j++) {
					ReferenceDescription rd = referenceDescriptions[j];
					if ((rd.getReferenceTypeId().equals(referenceTypeId))) {
						newReferenceDescriptions.add(rd);
					}
				}
				nextBrowseResult.setReferences(newReferenceDescriptions.toArray(new ReferenceDescription[newReferenceDescriptions.size()]));
			}
		}

		return nextBrowseResult;
	}

	/**
	 * Convenience method wrapping different reference types.
	 * 
	 * @param referenceTypeId
	 *          NodeId type to request.
	 * @return boolean true if parameter NodeId is some reference type. Otherwise return false.
	 */
	public static boolean isReferenceType(NodeId referenceTypeId) {

		if (referenceTypeId.equals(Identifiers.References) || referenceTypeId.equals(Identifiers.NonHierarchicalReferences) || referenceTypeId.equals(Identifiers.HierarchicalReferences)
		    || referenceTypeId.equals(Identifiers.HasEventSource) || referenceTypeId.equals(Identifiers.HasNotifier) || referenceTypeId.equals(Identifiers.Organizes)
		    || referenceTypeId.equals(Identifiers.HasChild) || referenceTypeId.equals(Identifiers.HasSubtype) || referenceTypeId.equals(Identifiers.Aggregates)
		    || referenceTypeId.equals(Identifiers.HasProperty) || referenceTypeId.equals(Identifiers.HasComponent) || referenceTypeId.equals(Identifiers.HasOrderedComponent)
		    || referenceTypeId.equals(Identifiers.GeneratesEvent) || referenceTypeId.equals(Identifiers.AlwaysGeneratesEvent) || referenceTypeId.equals(Identifiers.HasEncoding)
		    || referenceTypeId.equals(Identifiers.HasModellingRule) || referenceTypeId.equals(Identifiers.HasDescription) || referenceTypeId.equals(Identifiers.HasTypeDefinition)) {
			return true;
		}
		return false;

	}

}
