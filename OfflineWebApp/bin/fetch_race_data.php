<?php
//======================================================================================== 
//    HTML5 Webpage for the Capri 25 Offline Race Team
//	
//    fetch_race_data.php - script to fetch detail wind race data for one single race
//							and return it via json
//
//    (c) KaiserWare, Volker Petersen, January 2016
//
//======================================================================================== 
require "db_connect.php";

$return = array();
// get the wind data tables from the MYSQL database
if (isset($_GET["race"])) {
	$race_id = $_GET["race"];
	$wind_js = fetch_wind($race_id, "json");
	$return["status"]= "Success";
	$return["data"]  = $wind_js;
} else {
	$return["status"]= "Error";
	$return["data"]  = "no Race ID passed into this script.";
}
echo json_encode($return);  // return msg to Ajax
?>
