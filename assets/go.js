var margin = {top: 20, right: 20, bottom: 30, left: 50},
    width = 960 - margin.left - margin.right,
    height = 500 - margin.top - margin.bottom;

var formatDate = d3.time.format("%d-%b-%y");

var x = d3.time.scale()
    .range([0, width]);

var y = d3.scale.linear()
    .range([height, 0]);

var xAxis = d3.svg.axis()
    .scale(x)
    .orient("bottom");

var yAxis = d3.svg.axis()
    .scale(y)
    .orient("left");

var svg = d3.select("body").append("svg")
    .attr("width", width + margin.left + margin.right)
    .attr("height", height + margin.top + margin.bottom)
  .append("g")
    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

x.domain(d3.extent(data, function(d) { return d.iter; }));
y.domain(d3.extent(data, function(d) { return d.Xmodel[0][0]; }));

svg.append("g")
    .attr("class", "x axis")
    .attr("transform", "translate(0," + height + ")")
    .call(xAxis);

svg.append("g")
    .attr("class", "y axis")
    .call(yAxis)
    .append("text")
    .attr("transform", "rotate(-90)")
    .attr("y", 6)
    .attr("dy", ".71em")
    .style("text-anchor", "end")
    .text("Distance");

function addPoints(p, f) {
    svg.append("g")
    .selectAll(".symbol")
    .data(data)
    .enter()
    .append("text")
    .attr("class", "symbol")
    .html(function(d,i){
      return p;
    })
    .attr("x", function(d, i) {
        return x(d.iter) - 2;
    })
    .attr("y", function(d, i) {
        return y(f(d)) + 3;
    });
}

function addLine(color, dashes, f) {
    var line = d3.svg.line()
            .x(function(d) { return x(d.iter); })
            .y(function(d) { return y(f(d)); });


    svg.append("path")
        .datum(data)
        .attr("class", "line")
        .attr("stroke", color)
        .attr("stroke-dasharray", dashes)
        .attr("d", line);
}

addLine("green", "", function(d) { return d.Xmodel[0][0]; });
addLine("red", "(3,3)", function(d) { return d.Xk[0][0]; });
addPoints("&#xf083", function(d) { return d.Zk[0][0]; });
addPoints("&#x25cb;", function(d) { return d.Xk[0][0]; });
addPoints("x", function(d) { return d.Xmodel[0][0]; });


function type(d) {
    d.date = formatDate.parse(d.date);
    d.close = +d.close;
    return d;
}
