<!--
//======================================================================================== 
//    HTML5 Webpage for the Capri 25 Offline Race Team
//	
//    offline_header.php - common Menubar for all Offline website pages
//
//	  losely build upon the free, responsive Corlate template
//	
//    (c) KaiserWare, Volker Petersen, January 2016
//
//======================================================================================== 
-->

<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
	<meta name="keywords" content="Offline, Capri-25, One-Design, Racing, Sailing, Yachts" />
	<meta name="description" content="Capri-25 One-Design Racing" />
    <meta name="author" content="Offline Capri-25 Race Team" />
 	<meta name="robots" content="index,follow" />
 	<title><?php echo isset($title) ? $title : 'Offline Capri-25 Race Team'; ?></title>
    <link rel="shortcut icon" href="images/Offline.ico" />

	<!-- core CSS -->
	<!-- make sure the bootstrap css version play nice the the Bootstrap ls version (3.03)
	     that came with the Corlate template and only seem the function with the image slide on index.php -->
    <link href="css/bootstrap.min.css" rel="stylesheet" />
    <link href="css/bootstrap-social.css" rel="stylesheet" />
	<link href="css/jquery.dataTables.min.css" rel="stylesheet" />  
	<link href="css/responsive.dataTables.min.css" rel="stylesheet" />  
	<link href="css/font-awesome.min.css" rel="stylesheet" />
    <link href="css/animate.min.css" rel="stylesheet" />
    <link href="css/responsive.css" rel="stylesheet" />
    <link href="css/offline.css" rel="stylesheet" />
	<!--[if lt IE 9]>
    <script src="js/html5shiv.js"></script>
    <script src="js/respond.min.js"></script>
    <![endif]-->       

	<noscript>JavaScript must be enabled in order for you to use all features of this site.
      However, it seems JavaScript is either disabled or not supported by your browser. 
      To view all features of this site, please enable JavaScript by changing your browser 
	  options, and then try again.
	</noscript>
</head>

<body class="homepage">

<?php
$date_long = "l, F j, Y";  // Saturday, March 15, 2015
$date_short = "M. j, Y";  // March 15, 2015
$time_us = "g:ia";       // 7:30pm or 8:00am
$time_military = "H:i";   // 19:30 or 08:00
$mysqli = "";
?>

<!---------------------------------------------------------------------------------------------------------------
	Header bar definition 
----------------------------------------------------------------------------------------------------------------> 
<header id="header">
	<nav class="navbar navbar-default navbar-fixed-top" role="navigation">
		<div class="top-bar">
			<div class="container-fluid">
				<div class="row">
					<div class="col-xs-5 col-sm-3">
						<a href="index.php">
							<img class="img-fixed-size" src="images/OfflineLogoSineRed.png" alt="Offline Race Team logo" >
						</a>
					</div>
					<div class="col-xs-7 col-sm-9 social-share">
						<a class="btn btn-social-icon btn-sm btn-facebook" href="https://www.facebook.com/volker.petersen?ref=bookmarks" target="_blank">  <!-- btn-lg, btn, btn-sm, btn-xs -->
		 					<i class="fa fa-facebook"></i>
		  				</a> 
						<a class="btn btn-social-icon btn-sm btn-linkedin" href="https://www.linkedin.com/in/volkerpetersen" target="_blank">
		 					<i class="fa fa-linkedin"></i>
		  				</a>
						<button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
							<span class="sr-only">Toggle navigation</span>
							<span class="icon-bar"></span>
							<span class="icon-bar"></span>
							<span class="icon-bar"></span>
						</button>
					</div>
				</div><!-- row -->
			</div><!--/.container-->
		</div><!--/.top-bar-->
		<div class="container-fluid"> <!-- if you add:  id="navbar-header-rule" you get a rule with the container width 
									 if you want that, remove the <div id="navbar-header-rule"> at the end of this container -->
			<div class="row-fluid">			
				<div class="collapse navbar-collapse navbar-right">
					<ul id="MainMenu" class="nav navbar-nav">
						<li><a href="index.php">Home</a></li>
						<li><a href="wind.php">Wind Data</a></li>
						<li><a href="charts.php">Graphs</a></li>
					</ul>
				</div>
			</div>
		</div><!--/.container-->
	</nav><!--/nav-->
</header><!--/header-->


<!---------------------------------------------------------------------------------------------------------------
	Latest compiled and minified JavaScript for jQuery, DataTables, and Bootstrap
----------------------------------------------------------------------------------------------------------------> 
<script src="js/jquery.js"></script>
<script src="js/jquery.dataTables.min.js"></script>
<!-- use the old version (3.03) of bootstrap that came with the Corlate template -->
<script src="js/bootstrap.min.js"></script>
<script src="js/dataTables.responsive.min.js"></script>
<script src="js/jquery.isotope.min.js"></script>
<script src="js/wow.min.js"></script>

<!-- Add fancyBox 
<link rel="stylesheet" href="css/jquery.fancybox.css?v=2.1.5" type="text/css" media="screen" />
<script type="text/javascript" src="js/jquery.fancybox.pack.js?v=2.1.5"></script>
-->
<script type="text/javascript">
	var js_wind = new Array();  // global variable
	var avgAWDall, avgTWDall;   // global variables
</script>