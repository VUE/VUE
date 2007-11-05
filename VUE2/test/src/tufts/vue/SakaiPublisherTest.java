package tufts.vue;

import edu.tufts.osidimpl.repository.sakai.Type;
import edu.tufts.vue.dsm.DataSource;
import junit.framework.TestCase;

/**
 * @author  akumar03
 * @version $Revision: 1.2 $ / $Date: 2007-11-05 19:27:00 $ / $Author: mike $
 */

public class SakaiPublisherTest extends TestCase {

	edu.tufts.vue.dsm.DataSourceManager _dsMgr = null;
	edu.tufts.vue.dsm.DataSource _ds = null;  // this will be the first Sakai datasource found
	org.osid.shared.Type SakaiRepositoryType = 
		new edu.tufts.vue.util.Type("sakaiproject.org","repository","contentHosting");
	
	public SakaiPublisherTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		
		_dsMgr = edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance();
		((edu.tufts.vue.dsm.impl.VueDataSourceManager)_dsMgr).load();
		DataSource[] datasources = _dsMgr.getDataSources();
		for( int i = 0; i < datasources.length ; i++ )
		{
			//System.out.println(datasources[i].getRepositoryType().toString());
			if( datasources[i].getRepositoryType().isEqual(SakaiRepositoryType) )
			{
				_ds = datasources[i];
				System.out.println( "found this Sakai datasource: " + _ds.getRepositoryType().toString() );
				break;
			}
		}
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testUploadMap() {
		fail("Not yet implemented");
	}

	public void testUploadMapAll() {
		fail("Not yet implemented");
	}

	public void testGetSessionId() {
		fail("Not yet implemented");
	}

	public void testGetServerId() {
		fail("Not yet implemented");
	}

	public void testGetCookieString() {
	//	SakaiPublisher.getCookieString(_ds);
	}

}
