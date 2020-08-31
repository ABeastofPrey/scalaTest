describe("softMC Web Server test", function () {
	this.timeout(10000);
	require('./factoryRestore');
	describe("4453 User Management", () => {
		require('./UserManagement/ProfilePicture');
		require('./UserManagement/GetUserList');
		require('./UserManagement/RememberMe');
		require('./UserManagement/Delete');
		require('./UserManagement/TermsApproval');
		require('./UserManagement/Editing');
	});
	require('./ServerLog');
	describe("4461 File System Operations", () => {
		require('./FileSystemOperations/ListFile');
		require('./FileSystemOperations/Reading');
		require('./FileSystemOperations/Moving');
		require('./FileSystemOperations/Folders');
		require('./FileSystemOperations/Zipping');
		require('./FileSystemOperations/Deleting');
		require('./FileSystemOperations/FirmwareUpdate');
		require('./FileSystemOperations/Uploading(Legacy)');
		require('./FileSystemOperations/Uploading');
		require('./FileSystemOperations/Copying');
	});
	require('./BugReporting');
	require('./ProjectBackup');
	require('./Utils');
	require('./Recordings');
	require('./Sys.Info');
	require('./Websockets');

	after(() => {
		console.log('ALL TESTS COMPLETED - CHECK REPORT FOR RESULTS');
	});

});