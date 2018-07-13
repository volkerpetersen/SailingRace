<?php
//======================================================================================== 
//    testing
//======================================================================================== 

$return = array();
if (isset($_POST["user"])) {
	$return["status"]= "Success";
	$return["data"]  = array();
	$return["data"]["user"] = $_POST["user"];
	$return["data"]["pwd "] = $_POST["password"];
} else {
	$return["status"]= "Error";
	$return["data"]  = "NO user name or password given: ".json_encode($_POST);
}
echo json_encode($return);  // return msg to Android
?>
