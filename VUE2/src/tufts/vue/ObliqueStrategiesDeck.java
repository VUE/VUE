package tufts.vue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

public class ObliqueStrategiesDeck extends Hashtable {
	private static final Hashtable captions = new Hashtable();
	private static final int MAX_CARDS = 100;
	
	// Create a list to hold the card numbers
	private List cardnums = new ArrayList();
	// Create a list to hold the cards
	private List cards = new ArrayList();
	
	public ObliqueStrategiesDeck() {
		
		// first, populate our hashtable of captions
		populateCaptions();

		// Now put all the card numbers into the list
		// and shuffle them
		shuffleCardnums();
		
		// create a new deck of card (captions) in shuffle order
		setCardDeck();
				
	}
	
	private void populateCaptions() {
		// start with a clean slate
		captions.clear();
		
		captions.put(1, VueResources.getString("obliqueStrategies.label.1"));
		captions.put(2, VueResources.getString("obliqueStrategies.label.2"));
		captions.put(3, VueResources.getString("obliqueStrategies.label.3"));
		captions.put(4, VueResources.getString("obliqueStrategies.label.4"));
		captions.put(5, VueResources.getString("obliqueStrategies.label.5"));
		captions.put(6, VueResources.getString("obliqueStrategies.label.6"));
		captions.put(7, VueResources.getString("obliqueStrategies.label.7"));
		captions.put(8, VueResources.getString("obliqueStrategies.label.8"));
		captions.put(9, VueResources.getString("obliqueStrategies.label.9"));
		captions.put(10, VueResources.getString("obliqueStrategies.label.10"));
		captions.put(11, VueResources.getString("obliqueStrategies.label.11"));
		captions.put(12, VueResources.getString("obliqueStrategies.label.12"));
		captions.put(13, VueResources.getString("obliqueStrategies.label.13"));
		captions.put(14, VueResources.getString("obliqueStrategies.label.14"));
		captions.put(15, VueResources.getString("obliqueStrategies.label.15"));
		captions.put(16, VueResources.getString("obliqueStrategies.label.16"));
		captions.put(17, VueResources.getString("obliqueStrategies.label.17"));
		captions.put(18, VueResources.getString("obliqueStrategies.label.18"));
		captions.put(19, VueResources.getString("obliqueStrategies.label.19"));
		captions.put(20, VueResources.getString("obliqueStrategies.label.20"));
		captions.put(21, VueResources.getString("obliqueStrategies.label.21"));
		captions.put(22, VueResources.getString("obliqueStrategies.label.22"));
		captions.put(23, VueResources.getString("obliqueStrategies.label.23"));
		captions.put(24, VueResources.getString("obliqueStrategies.label.24"));
		captions.put(25, VueResources.getString("obliqueStrategies.label.25"));
		captions.put(26, VueResources.getString("obliqueStrategies.label.26"));
		captions.put(27, VueResources.getString("obliqueStrategies.label.27"));
		captions.put(28, VueResources.getString("obliqueStrategies.label.28"));
		captions.put(29, VueResources.getString("obliqueStrategies.label.29"));
		captions.put(30, VueResources.getString("obliqueStrategies.label.30"));
		captions.put(31, VueResources.getString("obliqueStrategies.label.31"));
		captions.put(32, VueResources.getString("obliqueStrategies.label.32"));
		captions.put(33, VueResources.getString("obliqueStrategies.label.33"));
		captions.put(34, VueResources.getString("obliqueStrategies.label.34"));
		captions.put(35, VueResources.getString("obliqueStrategies.label.35"));
		captions.put(36, VueResources.getString("obliqueStrategies.label.36"));
		captions.put(37, VueResources.getString("obliqueStrategies.label.37"));
		captions.put(38, VueResources.getString("obliqueStrategies.label.38"));
		captions.put(39, VueResources.getString("obliqueStrategies.label.39"));
		captions.put(40, VueResources.getString("obliqueStrategies.label.40"));
		captions.put(41, VueResources.getString("obliqueStrategies.label.41"));
		captions.put(42, VueResources.getString("obliqueStrategies.label.42"));
		captions.put(43, VueResources.getString("obliqueStrategies.label.43"));
		captions.put(44, VueResources.getString("obliqueStrategies.label.44"));
		captions.put(45, VueResources.getString("obliqueStrategies.label.45"));
		captions.put(46, VueResources.getString("obliqueStrategies.label.46"));
		captions.put(47, VueResources.getString("obliqueStrategies.label.47"));
		captions.put(48, VueResources.getString("obliqueStrategies.label.48"));
		captions.put(49, VueResources.getString("obliqueStrategies.label.49"));
		captions.put(50, VueResources.getString("obliqueStrategies.label.50"));
		captions.put(51, VueResources.getString("obliqueStrategies.label.51"));
		captions.put(52, VueResources.getString("obliqueStrategies.label.52"));
		captions.put(53, VueResources.getString("obliqueStrategies.label.53"));
		captions.put(54, VueResources.getString("obliqueStrategies.label.54"));
		captions.put(55, VueResources.getString("obliqueStrategies.label.55"));
		captions.put(56, VueResources.getString("obliqueStrategies.label.56"));
		captions.put(57, VueResources.getString("obliqueStrategies.label.57"));
		captions.put(58, VueResources.getString("obliqueStrategies.label.58"));
		captions.put(59, VueResources.getString("obliqueStrategies.label.59"));
		captions.put(60, VueResources.getString("obliqueStrategies.label.60"));
		captions.put(61, VueResources.getString("obliqueStrategies.label.61"));
		captions.put(62, VueResources.getString("obliqueStrategies.label.62"));
		captions.put(63, VueResources.getString("obliqueStrategies.label.63"));
		captions.put(64, VueResources.getString("obliqueStrategies.label.64"));
		captions.put(65, VueResources.getString("obliqueStrategies.label.65"));
		captions.put(66, VueResources.getString("obliqueStrategies.label.66"));
		captions.put(67, VueResources.getString("obliqueStrategies.label.67"));
		captions.put(68, VueResources.getString("obliqueStrategies.label.68"));
		captions.put(69, VueResources.getString("obliqueStrategies.label.69"));
		captions.put(70, VueResources.getString("obliqueStrategies.label.70"));
		captions.put(71, VueResources.getString("obliqueStrategies.label.71"));
		captions.put(72, VueResources.getString("obliqueStrategies.label.72"));
		captions.put(73, VueResources.getString("obliqueStrategies.label.73"));
		captions.put(74, VueResources.getString("obliqueStrategies.label.74"));
		captions.put(75, VueResources.getString("obliqueStrategies.label.75"));
		captions.put(76, VueResources.getString("obliqueStrategies.label.76"));
		captions.put(77, VueResources.getString("obliqueStrategies.label.77"));
		captions.put(78, VueResources.getString("obliqueStrategies.label.78"));
		captions.put(79, VueResources.getString("obliqueStrategies.label.79"));
		captions.put(80, VueResources.getString("obliqueStrategies.label.80"));
		captions.put(81, VueResources.getString("obliqueStrategies.label.81"));
		captions.put(82, VueResources.getString("obliqueStrategies.label.82"));
		captions.put(83, VueResources.getString("obliqueStrategies.label.83"));
		captions.put(84, VueResources.getString("obliqueStrategies.label.84"));
		captions.put(85, VueResources.getString("obliqueStrategies.label.85"));
		captions.put(86, VueResources.getString("obliqueStrategies.label.86"));
		captions.put(87, VueResources.getString("obliqueStrategies.label.87"));
		captions.put(88, VueResources.getString("obliqueStrategies.label.88"));
		captions.put(89, VueResources.getString("obliqueStrategies.label.89"));
		captions.put(90, VueResources.getString("obliqueStrategies.label.90"));
		captions.put(91, VueResources.getString("obliqueStrategies.label.91"));
		captions.put(92, VueResources.getString("obliqueStrategies.label.92"));
		captions.put(93, VueResources.getString("obliqueStrategies.label.93"));
		captions.put(94, VueResources.getString("obliqueStrategies.label.94"));
		captions.put(95, VueResources.getString("obliqueStrategies.label.95"));
		captions.put(96, VueResources.getString("obliqueStrategies.label.96"));
		captions.put(97, VueResources.getString("obliqueStrategies.label.97"));
		captions.put(98, VueResources.getString("obliqueStrategies.label.98"));
		captions.put(99, VueResources.getString("obliqueStrategies.label.99"));
		captions.put(100, VueResources.getString("obliqueStrategies.label.100"));		
	}
	
	public List getCardnums() {
		return cardnums;
	}
	
	// wipes the array of card numbers,
	// repopulates and shuffles them
	public void shuffleCardnums() {
		cardnums = new ArrayList();
		
		// Now put all the card numbers into the list
		for (int i=1; i<=MAX_CARDS; i++) {
			cardnums.add(i);
		}
		
		Collections.shuffle(cardnums);
	}
	
	public void setCardDeck() {
		this.clear();
		
		// create a node for each card in shuffle order
		Iterator iter = cardnums.iterator();
		while(iter.hasNext()) {
			int nextcardnum = (Integer) iter.next();
			String strNextLabel = captions.get(nextcardnum).toString();
			//LWNode nextNode = new LWObliqueNode(strNextLabel);
			this.put(nextcardnum, strNextLabel);				
		}
		
	} 
}
