package org.eclipse.jst.jsf.metadata.tests.taglibprocessing;

import junit.framework.Assert;

import org.eclipse.jst.jsf.metadataprocessors.internal.provisional.features.IDefaultValue;
import org.eclipse.jst.jsf.metadataprocessors.internal.provisional.features.IValidValues;

public class LongTypeTest extends TaglibProcessingTestCase {
	public void testPossibleValues(){		
		Assert.assertNotNull(possibleValueAdapters);
		Assert.assertTrue(possibleValueAdapters.isEmpty());
	}
	
	public void testValidValues(){		
		Assert.assertNotNull(validValuesAdapters);
		Assert.assertFalse(validValuesAdapters.isEmpty());
		
		IValidValues vv =(IValidValues)getProcessorForTaglibProcessingBundle(validValuesAdapters);
		Assert.assertTrue(vv.isValidValue("0"));
		Assert.assertTrue(vv.getValidationMessages().size()==0);
		vv.getValidationMessages().clear();
		Assert.assertTrue(vv.isValidValue("1000"));
		Assert.assertTrue(vv.isValidValue("-1000"));
		Assert.assertFalse(vv.isValidValue("-10L"));
		Assert.assertFalse(vv.isValidValue("False"));
		Assert.assertFalse(vv.getValidationMessages().size()==0);
		vv.getValidationMessages().clear();
		Assert.assertFalse(vv.isValidValue("-1001"));
		Assert.assertTrue(vv.getValidationMessages().size()==1);
		vv.getValidationMessages().clear();
		Assert.assertTrue(vv.isValidValue("-1"));
		Assert.assertTrue(vv.getValidationMessages().size()==0);
		vv.getValidationMessages().clear();
		Assert.assertFalse(vv.isValidValue("555555555555555555"));
		Assert.assertTrue(vv.getValidationMessages().size()==1);
		vv.getValidationMessages().clear();
		Assert.assertFalse(vv.isValidValue("555f9"));
		Assert.assertTrue(vv.getValidationMessages().size()==1);
		vv.getValidationMessages().clear();
		Assert.assertFalse(vv.isValidValue("2e1"));
		Assert.assertTrue(vv.getValidationMessages().size()==1);
	}
	
	public void testDefaultValues(){		
		Assert.assertNotNull(defaultValueAdapters);
		Assert.assertFalse(defaultValueAdapters.isEmpty());
		
		IDefaultValue dv =(IDefaultValue)getProcessorForTaglibProcessingBundle(defaultValueAdapters);
		Assert.assertTrue(dv.getDefaultValue() == null);
//		Assert.assertTrue(dv.getDefaultValue().equals("10"));
	}
	
	public void testCreateValues(){		
		Assert.assertNotNull(createValuesAdapters);
		Assert.assertTrue(createValuesAdapters.isEmpty());
	}
	
	
}
