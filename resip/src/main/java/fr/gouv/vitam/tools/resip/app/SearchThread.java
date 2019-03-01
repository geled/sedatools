/**
 * Copyright French Prime minister Office/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@programmevitam.fr
 * 
 * This software is developed as a validation helper tool, for constructing Submission Information Packages (archives 
 * sets) in the Vitam program whose purpose is to implement a digital archiving back-office system managing high 
 * volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA archiveTransfer the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.tools.resip.app;

import fr.gouv.vitam.tools.resip.data.Work;
import fr.gouv.vitam.tools.resip.frame.InOutDialog;
import fr.gouv.vitam.tools.resip.frame.MainWindow;
import fr.gouv.vitam.tools.resip.frame.SearchDialog;
import fr.gouv.vitam.tools.resip.parameters.Prefs;
import fr.gouv.vitam.tools.resip.utils.ResipLogger;
import fr.gouv.vitam.tools.resip.viewer.DataObjectPackageTreeModel;
import fr.gouv.vitam.tools.sedalib.core.ArchiveTransfer;
import fr.gouv.vitam.tools.sedalib.core.ArchiveUnit;
import fr.gouv.vitam.tools.sedalib.core.DataObjectPackage;
import fr.gouv.vitam.tools.sedalib.inout.exporter.ArchiveTransferToDiskExporter;
import fr.gouv.vitam.tools.sedalib.inout.exporter.ArchiveTransferToSIPExporter;
import fr.gouv.vitam.tools.sedalib.utils.SEDALibException;
import fr.gouv.vitam.tools.sedalib.utils.SEDALibProgressLogger;

import javax.swing.*;
import java.io.File;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

public class SearchThread extends SwingWorker<String, String> {

	private SearchDialog searchDialog;
	private ArchiveUnit searchUnit;
	private String searchExp;
	private DataObjectPackage dataObjectPackage;
	private DataObjectPackageTreeModel dataObjectPackageTreeModel;
	private List<ArchiveUnit> searchResult;

	public SearchThread(SearchDialog searchDialog,ArchiveUnit au) {
		this.searchDialog=searchDialog;
		this.searchUnit=au;
	}

	void searchInArchiveUnit(ArchiveUnit au){
		List<ArchiveUnit> auList=au.getChildrenAuList().getArchiveUnitList();

		for (ArchiveUnit childUnit:auList) {
			if (dataObjectPackage.isTouchedInDataObjectPackageId(childUnit.getInDataObjectPackageId()))
				continue;
			try {
				String tmp;
				if (searchDialog.metadataCheckBox.isSelected()) {
					tmp=childUnit.getContent().toString().replaceAll("<.*?>", "");
				}
				else
					tmp=dataObjectPackageTreeModel.findTreeNode(childUnit).getTitle();
				if (!searchDialog.caseCheckBox.isSelected()) tmp=tmp.toLowerCase();
				if (tmp.contains(searchExp))
					searchResult.add(childUnit);
				dataObjectPackage.addTouchedInDataObjectPackageId(childUnit.getInDataObjectPackageId());
			} catch (SEDALibException e) {
				// ignored
			}
			searchInArchiveUnit(childUnit);
		}
	}

	@Override
	public String doInBackground() {
		MainWindow mainWindow=(MainWindow)searchDialog.getParent();
		searchExp=searchDialog.searchTextField.getText();
		if (!searchDialog.caseCheckBox.isSelected()) searchExp=searchExp.toLowerCase();
		dataObjectPackageTreeModel=(DataObjectPackageTreeModel)mainWindow.getDataObjectPackageTreePaneViewer().getModel();
		dataObjectPackage=mainWindow.getApp().currentWork.getDataObjectPackage();
		dataObjectPackage.resetTouchedInDataObjectPackageIdMap();
		searchResult=new LinkedList<ArchiveUnit>();

		searchInArchiveUnit(searchUnit);
		return "OK";
	}

	@Override
	protected void done() {
		searchDialog.searchResult=searchResult;
		searchDialog.searchResultPosition=0;
		if (searchResult.size()>0) {
			searchDialog.resultLabel.setText("1/"+searchResult.size());
			searchDialog.focusArchiveUnit(searchResult.get(0));
         }
        else
			searchDialog.resultLabel.setText("0");
        searchDialog.searchRunning=false;
	}
}
