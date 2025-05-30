let currentSlide = 0;
let chart = null;
let airStagnationChart = null;
let precipitationChart = null;

function updateSlidePosition() {
    const slider = document.getElementById('dustSlider');
    if (!slider) return;
    const items = slider.querySelectorAll('.dust-forecast-item');
    slider.style.transform = `translateX(-${currentSlide * 100}%)`;
    const prevBtn = document.querySelector('.slider-btn.prev');
    const nextBtn = document.querySelector('.slider-btn.next');
    if (prevBtn) prevBtn.disabled = currentSlide === 0;
    if (nextBtn) nextBtn.disabled = currentSlide === items.length - 1;
}

function moveSlide(direction) {
    const items = document.querySelectorAll('.dust-forecast-item');
    currentSlide = Math.max(0, Math.min(currentSlide + direction, items.length - 1));
    updateSlidePosition();
}

function openDustForecastPopup() {
    const popup = document.getElementById('dustForecastPopup');
    popup.style.display = 'flex';
    currentSlide = 0;
    updateSlidePosition();
}

function closeDustForecastPopup() {
    document.getElementById('dustForecastPopup').style.display = 'none';
}

function openRealTimeDustPopup() {
    document.getElementById('realTimeDustPopup').style.display = 'flex';
}

function closeRealTimeDustPopup() {
    document.getElementById('realTimeDustPopup').style.display = 'none';
}

document.addEventListener('DOMContentLoaded', () => {
    const overallTexts = document.querySelectorAll('.overall-text');
    overallTexts.forEach(element => element.textContent = removeParentheses(element.textContent));
    const causeTexts = document.querySelectorAll('.cause-text');
    causeTexts.forEach(element => element.textContent = removeParentheses(element.textContent));

    const locationTitle = document.getElementById('locationTitle');
    if (!locationTitle.textContent.trim()) locationTitle.textContent = '청진동 (종로구)';

    const sidoSelect = document.getElementById('sido');
    const selectedSido = /*[[${selectedSido}]]*/ '';
    if (selectedSido && selectedSido !== '') {
        sidoSelect.value = selectedSido;
        updateSggs().then(() => {
            const sggSelect = document.getElementById('sgg');
            const selectedSgg = /*[[${selectedSgg}]]*/ '';
            if (selectedSgg && selectedSgg !== '') {
                sggSelect.value = selectedSgg;
                updateUmds().then(() => {
                    const umdSelect = document.getElementById('umd');
                    const selectedUmd = /*[[${selectedUmd}]]*/ '';
                    if (selectedUmd && selectedUmd !== '') umdSelect.value = selectedUmd;
                });
            }
        });
    }
});

function removeParentheses(text) {
    return text.replace(/\([^()]*\)/g, '').replace(/\[.*?\]/g, '').replace(/\./g, '').trim();
}

function openPopup(imageUrl) {
    closeDustForecastPopup();
    closeRealTimeDustPopup();
    closeLocationPopup();
    const popup = document.getElementById('imagePopup');
    document.getElementById('popupImage').src = imageUrl;
    popup.style.display = 'flex';
}

function closePopup() {
    document.getElementById('imagePopup').style.display = 'none';
}

function openLocationPopup() {
    document.getElementById('locationPopup').style.display = 'flex';
}

function closeLocationPopup() {
    document.getElementById('locationPopup').style.display = 'none';
}

async function updateSggs() {
    const sido = document.getElementById('sido').value;
    const sggSelect = document.getElementById('sgg');
    const umdSelect = document.getElementById('umd');

    sggSelect.innerHTML = '<option value="">선택하세요</option>';
    umdSelect.innerHTML = '<option value="">먼저 시/군/구를 선택하세요</option>';

    if (!sido) return;

    try {
        const response = await fetch(`/api/regions/sggs?sido=${encodeURIComponent(sido)}`);
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const sggs = await response.json();
        if (sggs.length === 0) {
            sggSelect.innerHTML = '<option value="">시/군/구 데이터 없음</option>';
        } else {
            sggs.forEach(sgg => {
                const option = document.createElement('option');
                option.value = sgg;
                option.textContent = sgg;
                sggSelect.appendChild(option);
            });
        }
    } catch (error) {
        console.error('시군구 데이터 가져오기 실패:', error);
        sggSelect.innerHTML = '<option value="">시/군/구 데이터를 불러올 수 없습니다</option>';
    }
}

async function updateUmds() {
    const sido = document.getElementById('sido').value;
    const sgg = document.getElementById('sgg').value;
    const umdSelect = document.getElementById('umd');

    umdSelect.innerHTML = '<option value="">선택하세요</option>';

    if (!sido || !sgg) return;

    try {
        const response = await fetch(`/api/regions/umds?sido=${encodeURIComponent(sido)}&sgg=${encodeURIComponent(sgg)}`);
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const umds = await response.json();
        if (umds.length === 0) {
            umdSelect.innerHTML = '<option value="">읍/면/동 데이터 없음</option>';
        } else {
            umds.forEach(umd => {
                const option = document.createElement('option');
                option.value = umd;
                option.textContent = umd;
                umdSelect.appendChild(option);
            });
        }
    } catch (error) {
        console.error('읍면동 데이터 가져오기 실패:', error);
        umdSelect.innerHTML = '<option value="">읍/면/동 데이터를 불러올 수 없습니다</option>';
    }
}

function confirmSelection() {
    const sido = document.getElementById('sido').value;
    const sgg = document.getElementById('sgg').value;
    const umd = document.getElementById('umd').value;

    if (!sido || !sgg || !umd) {
        alert('모든 항목을 선택해 주세요.');
        return;
    }

    document.getElementById('locationTitle').textContent = `${umd} (${sgg})`;
    closeLocationPopup();

    fetch(`/weather?location=${encodeURIComponent(`${sido} ${sgg} ${umd}`)}`)
        .then(response => {
            if (response.ok) {
                window.location.reload();
            } else {
                alert('날씨 데이터 업데이트하는 데 실패했습니다.');
            }
        })
        .catch(error => {
            console.error('날씨 데이터 업데이트 실패:', error);
            alert('날씨 데이터를 업데이트하는 데 실패했습니다.');
        });
}

// 차트 관련 JavaScript
async function fetchChartData() {
    try {
        const response = await fetch('/api/chart/temperature');
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const data = await response.json();
        return data;
    } catch (error) {
        console.error('차트 데이터 가져오기 실패:', error);
        return {
            startDate: getCurrentDateTimeFormatted(),
            temperatures: [18.0, 17.0, 18.0, 18.0, 18.0, 18.0, 18.0, 20.0, 21.0, 22.0]
        };
    }
}

function getCurrentDateTimeFormatted() {
    const now = new Date();
    const yyyy = now.getFullYear();
    const MM = String(now.getMonth() + 1).padStart(2, '0');
    const dd = String(now.getDate()).padStart(2, '0');
    const HH = String(now.getHours()).padStart(2, '0');
    return `${yyyy}${MM}${dd}${HH}`;
}

function createChart(startDate, temperatures) {
    const year = parseInt(startDate.substring(0, 4));
    const month = parseInt(startDate.substring(4, 6)) - 1;
    const day = parseInt(startDate.substring(6, 8));
    const hour = parseInt(startDate.substring(8, 10));
    const startTime = new Date(year, month, day, hour);
    const labels = [];
    for (let i = 0; i < temperatures.length; i++) {
        const time = new Date(startTime.getTime() + i * 60 * 60 * 1000);
        const label = `${time.getMonth() + 1}/${time.getDate()} ${time.getHours()}:00`;
        labels.push(label);
    }
    const ctx = document.getElementById('temperatureChart').getContext('2d');
    return new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: '체감 온도 (°C)',
                data: temperatures,
                borderColor: '#ff6b6b',
                backgroundColor: 'rgba(255, 107, 107, 0.2)',
                fill: true,
                tension: 0.4,
                pointRadius: 3,
                pointHoverRadius: 5
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: {
                    title: {
                        display: true,
                        text: '시간 (KST)',
                        font: { size: 16 }
                    },
                    ticks: {
                        maxTicksLimit: 12,
                        autoSkip: true,
                        font: { size: 12 }
                    }
                },
                y: {
                    title: {
                        display: true,
                        text: '온도 (°C)',
                        font: { size: 16 }
                    },
                    beginAtZero: false,
                    suggestedMin: 14,
                    suggestedMax: 30,
                    ticks: {
                        font: { size: 12 }
                    }
                }
            },
            plugins: {
                legend: {
                    display: true,
                    position: 'top',
                    labels: {
                        font: { size: 14 }
                    }
                },
                title: {
                    display: true,
                    text: '여름철 체감온도 예보 (5월~9월)',
                    font: { size: 20 }
                }
            }
        }
    });
}

async function toggleChart(button) {
    const chartContainer = document.querySelector('.chart-container');
    if (!chartContainer) return;

    button.classList.toggle('clicked');
    if (button.classList.contains('clicked')) {
        chartContainer.style.display = 'block';
        if (!chart) {
            try {
                const chartData = await fetchChartData();
                chart = createChart(chartData.startDate, chartData.temperatures);
            } catch (error) {
                console.error('차트 생성 실패:', error);
                alert('차트를 생성하는 데 실패했습니다.');
                chartContainer.style.display = 'none';
                button.classList.remove('clicked');
            }
        }
    } else {
        chartContainer.style.display = 'none';
    }
}

// 대기정체지수 차트 데이터 가져오기
async function fetchAirStagnationChartData() {
    try {
        const response = await fetch('/api/airchart/air-stagnation');
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const data = await response.json();
        return data;
    } catch (error) {
        console.error('대기정체지수 차트 데이터 가져오기 실패:', error);
        return {
            startDate: getCurrentDateTimeFormatted(),
            indices: [50, 55, 60, 65, 70, 75, 80, 85, 90, 95]
        };
    }
}

// 대기정체지수 차트 생성
function createAirStagnationChart(startDate, indices) {
    const year = parseInt(startDate.substring(0, 4));
    const month = parseInt(startDate.substring(4, 6)) - 1;
    const day = parseInt(startDate.substring(6, 8));
    const hour = parseInt(startDate.substring(8, 10));
    const startTime = new Date(year, month, day, hour);
    const labels = [];
    for (let i = 0; i < indices.length; i++) {
        const time = new Date(startTime.getTime() + i * 3 * 60 * 60 * 1000);
        const label = `${time.getMonth() + 1}/${time.getDate()} ${time.getHours()}:00`;
        labels.push(label);
    }
    const ctx = document.getElementById('airStagnationChart').getContext('2d');
    return new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: '대기정체지수',
                data: indices,
                borderColor: '#4a90e2',
                backgroundColor: 'rgba(74, 144, 226, 0.2)',
                fill: true,
                tension: 0.4,
                pointRadius: 3,
                pointHoverRadius: 5
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: {
                    title: {
                        display: true,
                        text: '시간 (KST)',
                        font: { size: 16 }
                    },
                    ticks: {
                        maxTicksLimit: 10,
                        autoSkip: true,
                        font: { size: 12 }
                    }
                },
                y: {
                    title: {
                        display: true,
                        text: '대기정체지수',
                        font: { size: 16 }
                    },
                    beginAtZero: false,
                    suggestedMin: 40,
                    suggestedMax: 110,
                    ticks: {
                        stepSize: 25,
                        font: { size: 12 }
                    }
                }
            },
            plugins: {
                legend: {
                    display: true,
                    position: 'top',
                    labels: {
                        font: { size: 14 }
                    }
                },
                title: {
                    display: true,
                    text: '대기정체지수 예보 (2025년 5월 27일 - 5월 31일)',
                    font: { size: 20 }
                }
            }
        }
    });
}

// 대기정체지수 차트 팝업 열기
async function openAirStagnationChartPopup() {
    const popup = document.getElementById('airStagnationChartPopup');
    popup.style.display = 'flex';

    if (!airStagnationChart) {
        try {
            const data = await fetchAirStagnationChartData();
            airStagnationChart = createAirStagnationChart(data.startDate, data.indices);
        } catch (error) {
            console.error('차트 데이터 가져오기 실패:', error);
            alert('대기정체지수 차트를 불러올 수 없습니다.');
            closeAirStagnationChartPopup();
        }
    }
}

// 대기정체지수 차트 팝업 닫기
function closeAirStagnationChartPopup() {
    document.getElementById('airStagnationChartPopup').style.display = 'none';
}

document.addEventListener('DOMContentLoaded', () => {
    const overallTexts = document.querySelectorAll('.overall-text');
    overallTexts.forEach(element => element.textContent = removeParentheses(element.textContent));
    const causeTexts = document.querySelectorAll('.cause-text');
    causeTexts.forEach(element => element.textContent = removeParentheses(element.textContent));

    const locationTitle = document.getElementById('locationTitle');
    if (!locationTitle.textContent.trim()) locationTitle.textContent = '청진동 (종로구)';

    const sidoSelect = document.getElementById('sido');
    const selectedSido = /*[[${selectedSido}]]*/ '';
    if (selectedSido && selectedSido !== '') {
        sidoSelect.value = selectedSido;
        updateSggs().then(() => {
            const sggSelect = document.getElementById('sgg');
            const selectedSgg = /*[[${selectedSgg}]]*/ '';
            if (selectedSgg && selectedSgg !== '') {
                sggSelect.value = selectedSgg;
                updateUmds().then(() => {
                    const umdSelect = document.getElementById('umd');
                    const selectedUmd = /*[[${selectedUmd}]]*/ '';
                    if (selectedUmd && selectedUmd !== '') umdSelect.value = selectedUmd;
                });
            }
        });
    }
});

async function fetchPrecipitationData() {
    try {
        const response = await fetch('/api/precipitation');
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const data = await response.json();
        return data;
    } catch (error) {
        console.error('강수량 데이터 가져오기 실패:', error);
        return {
            labels: ['5/30 11:00', '5/30 12:00', '5/30 13:00', '5/30 14:00', '5/30 15:00'],
            precipitations: [0.0, 0.5, 1.0, 0.5, 0.0]
        };
    }
}

function createPrecipitationChart(labels, precipitations) {
    const ctx = document.getElementById('precipitationChart').getContext('2d');
    if (precipitationChart) precipitationChart.destroy();
    precipitationChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: '강수량 (mm)',
                data: precipitations,
                backgroundColor: 'rgba(54, 162, 235, 0.6)',
                borderColor: '#36A2EB',
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            scales: {
                x: {
                    title: {
                        display: true,
                        text: '시간 (KST)',
                        font: { size: 14 }
                    }
                },
                y: {
                    title: {
                        display: true,
                        text: '강수량 (mm)',
                        font: { size: 14 }
                    },
                    beginAtZero: true,
                    suggestedMax: 5,
                    ticks: {
                        stepSize: 1
                    }
                }
            },
            plugins: {
                legend: {
                    display: true,
                    position: 'top'
                },
                title: {
                    display: true,
                    text: '강수량 예보 (5월 30일, 2025)',
                    font: { size: 18 }
                }
            }
        }
    });
}

async function openPrecipitationChartPopup() {
    const popup = document.getElementById('precipitationChartPopup');
    popup.style.display = 'flex';
    const data = await fetchPrecipitationData();
    createPrecipitationChart(data.labels, data.precipitations);
}

function closePrecipitationChartPopup() {
    document.getElementById('precipitationChartPopup').style.display = 'none';
    if (precipitationChart) {
        precipitationChart.destroy();
        precipitationChart = null;
    }
}

