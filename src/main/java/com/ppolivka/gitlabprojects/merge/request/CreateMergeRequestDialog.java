package com.ppolivka.gitlabprojects.merge.request;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.SortedComboBoxModel;
import com.ppolivka.gitlabprojects.component.SearchBoxModel;
import com.ppolivka.gitlabprojects.configuration.ProjectState;
import com.ppolivka.gitlabprojects.merge.info.BranchInfo;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dialog fore creating merge requests
 *
 * @author ppolivka
 * @since 30.10.2015
 */
public class CreateMergeRequestDialog extends DialogWrapper {

	private Project project;

	private JPanel mainView;
	private JComboBox targetBranch;
	private JLabel currentBranch;
	private JTextField mergeTitle;
	private JTextArea mergeDescription;
	private JButton diffButton;
	private JComboBox assigneeBox;
	private JCheckBox removeSourceBranch;
	private JCheckBox wip;

	private SortedComboBoxModel<BranchInfo> myBranchModel;
	private BranchInfo lastSelectedBranch;

	final ProjectState projectState;

	final GitLabCreateMergeRequestWorker mergeRequestWorker;

	public CreateMergeRequestDialog(@Nullable Project project, @NotNull GitLabCreateMergeRequestWorker gitLabMergeRequestWorker) {
		super(project);
		this.project = project;
		projectState = ProjectState.Companion.getInstance(project);
		mergeRequestWorker = gitLabMergeRequestWorker;
		init();

	}

	@Override
	protected void init() {
		super.init();
		setTitle("Create Merge Request");
		setVerticalStretch(2f);

		SearchBoxModel searchBoxModel = new SearchBoxModel(assigneeBox, mergeRequestWorker.searchableUsers);
		assigneeBox.setModel(searchBoxModel);
		assigneeBox.setEditable(true);
		assigneeBox.addItemListener(searchBoxModel);
		assigneeBox.setBounds(140, 170, 180, 20);

		currentBranch.setText(mergeRequestWorker.gitLocalBranch.getName());

		myBranchModel = new SortedComboBoxModel<>((o1, o2) -> StringUtil.naturalCompare(o1.getName(), o2.getName()));
		myBranchModel.setAll(mergeRequestWorker.branches);
		targetBranch.setModel(myBranchModel);
		targetBranch.setSelectedIndex(0);
		if (mergeRequestWorker.lastUsedBranch != null) {
			targetBranch.setSelectedItem(mergeRequestWorker.lastUsedBranch);
		}
		lastSelectedBranch = getSelectedBranch();

		targetBranch.addActionListener(e -> {
			prepareTitle();
			lastSelectedBranch = getSelectedBranch();
			projectState.setLastMergedBranch(getSelectedBranch().getName());
			mergeRequestWorker.getDiffViewWorker().launchLoadDiffInfo(mergeRequestWorker.localBranchInfo, getSelectedBranch());
		});

		prepareTitle();

		Boolean deleteMergedBranch = projectState.getDeleteMergedBranch();
		if (deleteMergedBranch != null && deleteMergedBranch) {
			this.removeSourceBranch.setSelected(true);
		}

		Boolean mergeAsWorkInProgress = projectState.getMergeAsWorkInProgress();
		if (mergeAsWorkInProgress != null && mergeAsWorkInProgress) {
			this.wip.setSelected(true);
		}

		diffButton.addActionListener(
			e -> mergeRequestWorker.getDiffViewWorker().showDiffDialog(mergeRequestWorker.localBranchInfo, getSelectedBranch()));
	}

	@Override
	protected void doOKAction() {
		BranchInfo branch = getSelectedBranch();
		if (mergeRequestWorker.checkAction(branch)) {
			String title = mergeTitle.getText();
			if (wip.isSelected()) {
				title = "WIP:" + title;
			}
			mergeRequestWorker.createMergeRequest(branch, getAssignee(), title, mergeDescription.getText(), removeSourceBranch.isSelected());
			super.doOKAction();
		}
	}

	@Nullable
	@Override
	protected ValidationInfo doValidate() {
		if (StringUtils.isBlank(mergeTitle.getText())) {
			return new ValidationInfo("Merge title cannot be empty", mergeTitle);
		}
		if (getSelectedBranch().getName().equals(currentBranch.getText())) {
			return new ValidationInfo("Target branch must be different from current branch.", targetBranch);
		}
		return null;
	}

	private BranchInfo getSelectedBranch() {
		return (BranchInfo) targetBranch.getSelectedItem();
	}

	@Nullable
	private GitlabUser getAssignee() {
		SearchableUser searchableUser = (SearchableUser) this.assigneeBox.getSelectedItem();
		if (searchableUser != null) {
			return searchableUser.getGitLabUser();
		}
		return null;
	}

	private void prepareTitle() {
		if (StringUtils.isBlank(mergeTitle.getText()) || mergeTitleGenerator(lastSelectedBranch).equals(mergeTitle.getText())) {
			mergeTitle.setText(mergeTitleGenerator(getSelectedBranch()));
		}
	}

	private String mergeTitleGenerator(BranchInfo branchInfo) {
		return "Merge of " + currentBranch.getText() + " to " + branchInfo;
	}

	@Nullable
	@Override
	protected JComponent createCenterPanel() {
		return mainView;
	}
}
