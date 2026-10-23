document.addEventListener("click", function (event) {
  var button = event.target.closest("[data-confirm]");
  if (button && !window.confirm(button.getAttribute("data-confirm"))) {
    event.preventDefault();
  }
});
document.querySelectorAll("input[type=file][data-preview]").forEach(function (input) {
  input.addEventListener("change", function () {
    var target = document.querySelector(input.getAttribute("data-preview"));
    if (target) {
      target.textContent = input.files.length ? input.files[0].name : "";
    }
  });
});
function drawChart(canvasId, url, type) {
  var canvas = document.getElementById(canvasId);
  if (!canvas || typeof Chart === "undefined") {
    return;
  }
  fetch(url, { headers: { "Accept": "application/json" } })
    .then(function (response) { return response.json(); })
    .then(function (series) {
      new Chart(canvas, {
        type: type || "line",
        data: {
          labels: series.labels,
          datasets: [{ label: "INR", data: series.amounts, borderColor: "#9a3412", backgroundColor: "#fdba74" }]
        },
        options: { responsive: true }
      });
    });
}
