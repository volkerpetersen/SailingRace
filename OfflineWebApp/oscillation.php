<!DOCTYPE html>
<!--
//======================================================================================== 
//    HTML5 Webpage for the Capri 25 Offline Race Team
//	
//    oscillation.php - plot the wind oscillation analysis using Google Chart API
//
//	  losely build upon the free, responsive Corlate template
//	
//    (c) KaiserWare, Volker Petersen, July 2016
//
//======================================================================================== 
-->
<?php 
global $title;
$race_id;
$title = 'Offline - Wind Oscillations';
require "offline_header.php";
require "bin/db_connect.php";


?>
<script type="text/javascript" src="https://www.google.com/jsapi"></script>

<section id="feature" class="body-page-start">
	<div class="container">
		<div class="row">
		    <div class="col-xs-12 text-center">
				<h1>Wind Oscillation Analysis</h1>
				<h4 id="chart_title"></h4>
			</div>
		</div>
		<div class="row">
				<!-- Google Chart goes in div with id=char_div -->
			<div class="col-xs-12 chart_div">
				<div id="chart_div" style="width: 100%; height: 650px;"></div>
			</div>
		</div>
		<div class="row">
			<div class="col-xs-12">&nbsp;<br />&nbsp;</div>
		</div>
	</div>
</section>


<script type='text/javascript'>
	function plot_chart(chart_data, options, title, legend) {
	
        options.chartArea = {left: 70, right: 20, top: 40, bottom: 1, width: '100%', height: '80%'};
		options.hAxis = {
			//format: 'yyyy-M-dd HH:mm',
			format: '0',
			title: 'Race Date / Time (UTC)'
		};
		options.vAxis = {
			format: '0',
			title: 'True Wind Direction (Magnetic)'
		};
		if (legend) {
			options.legend = {
				position: 'top', 
				alignment: 'center'
			};
		}
		options.smoothLine = true;
		document.getElementById("chart_title").innerHTML = title;
		var chart = new google.visualization.LineChart(document.getElementById('chart_div'));
		chart.draw(chart_data, options);
	}

	function all_lines(legend) {
		var chart_data = new google.visualization.DataTable();
		chart_data.addColumn('datetime', 'Time of Day');
		chart_data.addColumn('number', 'Q1');
		chart_data.addColumn('number', 'Q2');
		chart_data.addColumn('number', 'Q3');
		chart_data.addColumn('number', 'Q4');
		chart_data.addColumn('number', 'All');
		for (var i=0; i < js_wind.length; i++){
			var row = [];
			for (var j = 0; j < js_wind[i].length; j++) {
				if (j == 0) {
					row.push(new Date(Date.parse(js_wind[i][j])));
					//console.log("all_lines: "+js_wind[i][j]+" | "+Date.parse(js_wind[i][j]));
				} else {
					row.push(js_wind[i][j]);
				}
			}
			chart_data.addRow(row);
		}
        var options = {
          series: {
            0: { color: 'FF0000' },
            1: { color: '00FF00' },
            2: { color: '0000FF' },
            3: { color: 'FFAA33' },
            4: { color: '000000', lineDashStyle: [10, 5] }
          }
        };
        var title = 'Quadrants 1, 2, 3, & 4 plus entire Race course wind data';
		plot_chart(chart_data, options, title, legend);
	}

	function plot_wind_oscillation(legend) {
		var TWD = new Array(294.0, 288.5, 293.8, 295.8, 299.4, 298.5, 300.0, 301.8, 303.0,
                294.7, 296.9, 289.7, 293.3, 284.7, 288.3, 283.1, 284.3, 285.0,
                281.2, 283.0, 277.5, 278.6, 279.2, 285.2, 285.5, 292.0, 296.5,
                291.8, 297.8, 295.4, 302.5, 297.0, 296.8, 298.0, 300.7, 298.9,
                290.7, 287.3, 285.7, 288.3, 288.1, 280.3, 277.0, 280.2, 280.0,
                276.5, 280.6, 283.2, 284.2, 287.5, 288.0, 296.5, 293.8, 296.8,
                301.4, 298.5, 298.0, 298.8, 302.0, 297.7, 292.9, 296.7, 290.3,
                285.7, 284.3, 284.1, 280.3, 277.0, 284.2, 282.0, 278.5, 282.6,
                286.2, 287.2, 283.5, 289.0, 296.5, 293.8, 293.8, 302.4, 295.5,
                298.0, 295.8, 301.0, 295.7, 291.9, 289.7, 289.3, 284.7, 283.3,
                285.1, 283.3, 279.0, 281.2, 280.0, 284.5, 281.6, 282.2, 288.2,
                283.5, 286.0, 289.5, 297.8, 296.8, 301.4, 296.5, 304.0, 301.8,
                302.0, 293.7, 298.9, 289.7, 287.3, 288.7, 282.3, 285.1, 282.3,
                280.0, 283.2, 283.0, 280.5, 282.6, 287.2, 287.2, 291.5, 288.0,
                294.5, 295.8, 296.8, 299.4, 297.5, 298.0, 303.8, 303.0, 293.7,
                292.9, 290.7, 295.3, 292.7, 290.3, 281.1, 279.3, 280.0, 279.2,
                281.0, 283.5, 278.6, 281.2, 288.2, 286.5, 291.0, 292.5, 291.8,
                294.8, 301.4, 298.5, 298.0, 303.8, 298.0, 298.7, 294.9, 292.7,
                288.3, 284.7, 285.3, 285.1, 286.3, 281.0, 278.2, 282.0, 276.5,
                281.6, 281.2, 283.2, 290.5, 294.0, 296.5, 292.8, 300.8, 295.4,
                300.5, 297.0, 297.8, 295.0, 301.7, 292.9, 294.7, 289.3, 287.7,
                288.3, 283.1, 284.3, 285.0, 282.2, 280.0, 282.5, 278.6, 285.2,
                281.2, 290.5, 293.0);

		var smoothedTWD = TWD[0];
		var alpha = 0.3;
		var chart_data = new google.visualization.DataTable();
		chart_data.addColumn('number', 'data value #');
		chart_data.addColumn('number', 'TWD');
		chart_data.addColumn('number', 'smoothed TWD');
		chart_data.addColumn('number', 'Average');
		var avgTWD = 0.0;
		for (var i=0; i < TWD.length; i++){
			avgTWD = avgTWD + TWD[i];
		}
		avgTWD = avgTWD / TWD.length;
		for (var i=1; i < TWD.length; i++){
			smoothedTWD = alpha*TWD[i] + (1.0-alpha)*smoothedTWD;
			chart_data.addRow([i, TWD[i], smoothedTWD, avgTWD]);
			//console.log("all Q: "+js_wind[i][0]+" | "+new Date(Date.parse(js_wind[i][0])));
		}
        var options = {
          series: {
            0: { color: 'FF0000' },
            1: { color: '00FF00' },
            2: { color: '0000FF' }
           }
        };
        var title = 'Wind Oscillation Data';
		plot_chart(chart_data, options, title, legend);
	}


	//==============================================================================================================
	// end of the function defitions
	//==============================================================================================================


	// default code run on launch of this script

	google.load("visualization", "1", {packages:["corechart"]});
	jQuery(document).ready(function(){
		google.setOnLoadCallback(plot_wind_oscillation(true));
 	}); // end of jQuery(document).ready(function()

</script>

<?php
require "offline_footer.php"; 
?>
