/*
 * Copyright 2003-2008 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package edu.tufts.vue.ui;

public class ImageAssetViewer
implements edu.tufts.vue.fsm.AssetViewer
{
	org.osid.shared.Type mediumImageType = new edu.tufts.vue.util.Type("mit.edu","partStructure","mediumImage");
	org.osid.shared.Type thumbnailType = new edu.tufts.vue.util.Type("mit.edu","partStructure","thumbnail");
	
	public java.awt.Component viewAsset(org.osid.repository.Asset asset) {
		javax.swing.JPanel panel = new javax.swing.JPanel();
		try {
			org.osid.repository.RecordIterator recordIterator = asset.getRecords();
			while (recordIterator.hasNextRecord()) {
				org.osid.repository.PartIterator partIterator = recordIterator.nextRecord().getParts();
				while (partIterator.hasNextPart()) {
					org.osid.repository.Part part = partIterator.nextPart();
					if (mediumImageType.isEqual(part.getPartStructure().getType())) {
						javax.swing.ImageIcon icon = new javax.swing.ImageIcon((String)part.getValue());
						panel.add(new javax.swing.JLabel(icon));
					}
				}
			}
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"preapring asset viewer");
		}
		return panel;
	}
	
	public java.awt.Component viewAssets(org.osid.repository.AssetIterator assetIterator) {
		javax.swing.JPanel panel = new javax.swing.JPanel();
		try {
			while (assetIterator.hasNextAsset()) {
				org.osid.repository.RecordIterator recordIterator = assetIterator.nextAsset().getRecords();
				while (recordIterator.hasNextRecord()) {
					org.osid.repository.PartIterator partIterator = recordIterator.nextRecord().getParts();
					while (partIterator.hasNextPart()) {
						org.osid.repository.Part part = partIterator.nextPart();
						if (thumbnailType.isEqual(part.getPartStructure().getType())) {
							javax.swing.ImageIcon icon = new javax.swing.ImageIcon((String)part.getValue());
							panel.add(new javax.swing.JLabel(icon));
						}
					}
				}
			}
		} catch (Throwable t) {
			edu.tufts.vue.util.Logger.log(t,"preapring asset viewer");
		}
		return panel;
	}
}
