<!DOCTYPE html>
<!--
//======================================================================================== 
//    HTML5 Webpage for the Capri 25 Offline Race Team
//	
//    wind.php - Offline Capri-25 wind data
//
//	  losely build upon the free, responsive Corlate template
//	
//    (c) KaiserWare, Volker Petersen, January 2016
//
//======================================================================================== 
-->
<?php 
global $title;
global $wind;
$wind = array();
$title = 'Offline Capri-25 Race Team';
require "offline_header.php";
require "bin/db_connect.php";

// get the data from the MYSQL database
$wind_js = fetch_wind("", "js");
$race_js = fetch_races();

?>
<section id="feature" class="body-page-start">
	<div class="container">
		<div class="row">
			<div class="col-xs-12 col-sm-6 text-center">
				<h1 id="table_name" class="text-info">Detailed Wind Records</h1>
			</div>
			<div class="col-xs-12 visible-xs text-center">
				<button class="btn btn-donation" onclick="showWindDetail()"><b>Detailed Wind Data</b></button>
			</div>
			<div class="col-xs-12 visible-xs text-center">
				<button class="btn btn-donation" onclick="showRaceSummary()"><b>Summary Race Wind Data</b></button>
			</div>
			<div class="hidden-xs col-sm-6 text-right">
				<h1>
				<button class="btn btn-donation" onclick="showWindDetail()"><b>Detailed Wind Data</b></button>&nbsp;&nbsp;
				<button class="btn btn-donation" onclick="showRaceSummary()"><b>Summary Race Wind Data</b></button>
				</h1>
			</div>
		</div>

		<!-- Body content goes here, content provided by the javascript below -->
        <div class="row">
			<div class="col-xs-12 col-sm-offset-1 col-sm-10" id="windTable" >
			</div>
		</div><!--/.row-->

		&nbsp;<BR />
	</div><!--/.container-->
</section><!--/#feature-->
<script type="text/javascript">
	<?php 
		echo($wind_js);
		echo($race_js);
	 ?>
	showRaceSummary();

	function showWindDetail() {
		var table_name = "Detailed Wind Records";
		var windTable = '<table id="dataTable" class="display responsive nowrap" cellspacing="0" width="100%">';
		windTable += '<thead><tr><th class="sorting_asc col-xs-4">Date</th>';
		windTable += '<th class="sorting col-xs-1">AWD</th>';
		windTable += '<th class="col-xs-1">AWS</th>';
		windTable += '<th class="sorting col-xs-1">TWD</th>';
		windTable += '<th class="col-xs-1">TWS</th>';
		windTable += '<th class="sorting col-xs-2">Quadrant</th>';
		windTable += '<th class="sorting_asc col-xs-2">Race</th></tr></thead>';
		windTable += '<tfoot><tr><th class="sorting_asc col-xs-4">Date</th>';
		windTable += '<th class="sorting col-xs-1">AWD</th>';
		windTable += '<th class="col-xs-1">AWS</th>';
		windTable += '<th class="sorting col-xs-1">TWD</th>';
		windTable += '<th class="col-xs-1">TWS</th>';
		windTable += '<th class="sorting col-xs-2">Quadrant</th>';
		windTable += '<th class="sorting_asc col-xs-2">Race</th></tr></tfoot><tbody>';
		for (var i = 0; i < wind.length; i++) {
			windTable += '<tr>';
			for (var j = 0; j < wind[i].length; j++) {
				windTable += '<td class="text-left">'+wind[i][j]+'</td>';
			}
			windTable += "</tr>";
		}
		windTable += "</tbody</table>&nbsp;<br />";
		showTable(table_name, windTable, 4, "asc", 0, "asc");
	}

	function showRaceSummary() {
		var table_name = "Summary Race Wind Records";
		var windTable = '<table id="dataTable" class="table row-border hover display responsive nowrap" cellspacing="0" width="100%">';
		windTable += '<thead><tr><th class="sorting col-xs-2">Date</th>';
		windTable += '<th class="sorting col-xs-1">TWS</th>';
		windTable += '<th class="sorting col-xs-1">TWD</th>';
		windTable += '<th class="sorting col-xs-2">WWD LAT</th>';
		windTable += '<th class="sorting col-xs-2">WWD LON</th>';
		windTable += '<th class="sorting col-xs-2">LWD LAT</th>';
		windTable += '<th class="sorting col-xs-2">LWD LON</th></tr></thead>';
		windTable += '<tfoot><tr><th class="sorting_asc col-xs-2">Race ID</th>';
		windTable += '<th class="sorting col-xs-1">TWS</th>';
		windTable += '<th class="sorting col-xs-1">TWD</th>';
		windTable += '<th class="sorting col-xs-2">WWD LAT</th>';
		windTable += '<th class="sorting col-xs-2">WWD LON</th>';
		windTable += '<th class="sorting col-xs-2">LWD LAT</th>';
		windTable += '<th class="sorting col-xs-2">LWD LON</th></tr></tfoot><tbody>';
		for (var i = 0; i < race.length; i++) {
			windTable += '<tr>';
			for (var j = 0; j < race[i].length-1; j++) {
				if (j == 0) {
					//console.log('raceDate: '+race[i][race[i].length-1]);
					windTable += '<td class="text-left"><a href="charts.php?race='+race[i][race[i].length-1];
					windTable += '">'+race[i][j]+'</td>';

				} else {
					windTable += '<td class="text-left">'+race[i][j]+'</td>';
				}
			}
			windTable += "</tr>";
		}
		windTable += "</tbody</table>&nbsp;<br />";
		showTable(table_name, windTable, 0, "desc", 2, "asc");
	}

	function showTable(name, data, first_col, first_order, second_col, second_order) {
		document.getElementById("table_name").innerHTML = name;
		document.getElementById("windTable").innerHTML = data;
		jQuery('#dataTable').DataTable({
			"paging": true,     // enable paging
			"responsive": true, // enable responsive behavior
			"bFilter": true,    // enable the search box
			"lengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
	        "order": [[first_col, first_order ], [second_col, second_order ]]
		});
	}
</script>
<?php
require "offline_footer.php"; 
?>