<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<asset:javascript src="kpiOverall"/>
 <!-- Tendency Charts -->
 
<div class="row" id="mainTendency" style="margin-top:20px;height:500px;">
<ul class="nav nav-tabs tendencyList">
				<li class="tab1 kpiTab" role="presentation" id="show5">Five Years Tendency</li>
				<li class="tab2 kpiTab" role="presentation" id="showYear">Current Year Tendency</li>
</ul>
<div id="tendency5y"></div>
<div id="tendencyYear" style="display:none;width:85%"></div>
</div><!-- Tendency Charts End -->






<!-- Pie Charts --> 
 <div class="row" style="margin-top:100px">
 	
 	<div class="col-md-4" id="byClas"></div>
 	<div class="col-md-4" id="byStatus"></div>
 	<div class="col-md-4" id="byEvent"></div>
 </div><!-- Pie charts End -->
 
 
 
 

<g:javascript>
	$(function () {
	/*Five Year Tendency Chart*/
	 Highcharts.setOptions({
	colors: ['rgba(178, 9, 56, 0.85)', 'rgba(253, 184, 20, 0.85)', 'rgba(70, 70, 69, 0.85)', '#f7a35c', '#8085e9', 
   '#f15c80', '#e4d354', '#2b908f', '#f45b5b', '#91e8e1']
   });
    $('#tendency5y').highcharts({
        chart: {
            type: 'line',
            backgroundColor: 'rgba(255,255,255,0.7)'
        },
        title: {
            text: 'Five-Year Total Service Hours Tendency',
            style: {  
                color: '#B20938',
                fontWeight: 'bold',
                fontSize:'20px'
            }
        },
         credits: {
            enabled: false
        },
        xAxis: {
            categories: ['2011','2012','2013','2014','2015']
        },
        yAxis: {
            title: {
                text: 'Service Hours(hr)'
            }
        },
        tooltip: {
        	headerFormat:'<span style="font-size:2em;text-align:center"><b>{point.key}</b></span><br/>',
            pointFormat: '<b  style="color:{series.color}">{series.name}: </b>{point.y}<br/>',
            valueSuffix: ' hr',
            shared: true,
            valueDecimals: 2
        },
        plotOptions: {
            series: {
                cursor: 'pointer',
                events: {
                    click: function () {
                        alert('You just clicked the graph');
                    }
                }
            }
        },
        series: [{
            name: 'Actual',
            data: [${tendency5y.get('totalLast4')},${tendency5y.get('totalLast3')},${tendency5y.get('totalLast2')},${tendency5y.get('totalLast1')},${tendency5y.get('totalYear')}]
       		
        },{
        	name:'Average',
        	data:[${tendency5y.get('average')},${tendency5y.get('average')},${tendency5y.get('average')},${tendency5y.get('average')},${tendency5y.get('average')}],
       		marker: {
                symbol: 'circle',
            }
        }]
    });
    
    /*Current Year Tendency Chart*/
     $('#tendencyYear').highcharts({
        chart: {
            type: 'column',
            backgroundColor: 'rgba(255,255,255,0.7)'
        },
        title: {
            text: 'Monthly Service Hour Tendency',
            style: {  
                color: '#B20938',
                fontWeight: 'bold',
                fontSize:'20px'
            }
        },
        subtitle: {
            text: '2015'
        },
         credits: {
            enabled: false
        },
        xAxis: {
            categories: ['1','2','3','4','5','6','7','8','9','10','11','12']
        },
        yAxis: {
            title: {
                text: 'Service Hours(hr)'
            }
        },
        tooltip: {
        	headerFormat:'<span style="font-size:2em;text-align:center"><b>{point.key}</b></span><br/>',
            pointFormat: '<b  style="color:{series.color}">{series.name}: </b>{point.y}<br/>',
            valueSuffix: ' hr',
            shared: true,
            valueDecimals: 2
        },
        plotOptions: {
            series: {
                cursor: 'pointer',
                events: {
                    click: function () {
                        alert('You just clicked the graph');
                    }
                }
            }
        },
        series: [{
            name: 'Actual',
            data: [${tendencyYear[0]},${tendencyYear[1]},${tendencyYear[2]},${tendencyYear[3]},${tendencyYear[4]},
            ${tendencyYear[5]},${tendencyYear[6]},${tendencyYear[7]},${tendencyYear[8]},${tendencyYear[9]},${tendencyYear[10]},
				${tendencyYear[11]}]
        },{
        	name:'Expected',
        	data:[1,2,3,3,3,2,1,1,3,4,3,1],
        	marker: {
                symbol: 'circle',
            }
        },
        {
        	name:"Average",
        	data:[${tendencyYear[12]},${tendencyYear[12]},${tendencyYear[12]},${tendencyYear[12]},${tendencyYear[12]},${tendencyYear[12]},${tendencyYear[12]},${tendencyYear[12]},${tendencyYear[12]},${tendencyYear[12]},${tendencyYear[12]},${tendencyYear[12]}],
        	marker: {
                symbol: 'circle',
            }
        }]
    });
    
  

    // Build the chart
    $('#byStatus').highcharts({
        chart: {
            plotBackgroundColor: null,
            plotBorderWidth: null,
            plotShadow: false,
            type: 'pie',
            backgroundColor: 'rgba(255,255,255,0.7)'
        },
        title: {
            text: 'Service Hours by Status'
        },
        credits: {
            enabled: false
        },
        tooltip: {
            pointFormat: '{series.name}: <b>{point.percentage:.1f}%</b>'
        },
        plotOptions: {
            pie: {
                allowPointSelect: true,
                cursor: 'pointer',
                dataLabels: {
                    enabled: true,
                    format: '<b>{point.name}</b> <br/>{point.percentage:.1f}%',
                    distance: -50,
                    style: {
                        color: (Highcharts.theme && Highcharts.theme.contrastTextColor) || 'white',
                        fontSize:'15px',
                        textShadow: 'none'
                    }
                }
            }
        },
        series: [{
            name: 'Classification',
            data: [
                { name: 'APPROVED', y: ${chartStatus.aSum },color:'#5CB85C'},
                { name: 'PENDING', y: ${chartStatus.pSum },color:'#F0AD4E'},
                { name: 'REJECTED', y: ${chartStatus.rSum },color:'#777777' }
            ]
        }]
    });
    
    
     $('#byClas').highcharts({
        chart: {
            type: 'pie',
            backgroundColor: 'rgba(255,255,255,0.7)'
        },
        title: {
            text: 'Service Hours by Classification'
        },
        credits: {
            enabled: false
        },
        tooltip: {
            pointFormat: '{series.name}: <b>{point.percentage:.1f}%</b>'
        },
        plotOptions: {
            pie: {
                allowPointSelect: true,
                cursor: 'pointer',
                dataLabels: {
                    enabled: true,
                    format: '<b>{point.name}</b><br/>{point.percentage:.1f}%',
                    distance: -50,
                    style: {
                        color: 'white',
                        fontSize:'15px',
                        textShadow: 'none'
                    },
                    connectorColor: 'silver'
                    
                }
            }
        },
        series: [{
            name: 'Classification',
            data: [
                { name: 'FR', y: ${chartClas.frSum } },
                { name: 'SO', y: ${chartClas.soSum } },
                { name: 'JU', y: ${chartClas.juSum } }, 
                { name: 'SE', y: ${chartClas.seSum } },
                { name: 'GRAD', y: ${chartClas.gradSum} },
                { name:'OTHER', y:${chartClas.otherSum}}
            ]
        }]
    });
});
</g:javascript>
</html>