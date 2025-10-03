let currentSlide = 0;
let chart = null;
let airStagnationChart = null;
let precipitationChart = null;
let temperatureChart = null;
let weatherStatsChart = null;
let dailyChart = null;
let threeDayChart = null;
let selectedDay = 0;
let weatherData = null;

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
    loadSidos();
}

function closeLocationPopup() {
    document.getElementById('locationPopup').style.display = 'none';
}

async function loadSidos() {
    const sidoSelect = document.getElementById('sido');
    const sggSelect = document.getElementById('sgg');
    const umdSelect = document.getElementById('umd');

    sggSelect.innerHTML = '<option value="">시/군/구를 먼저 선택하세요</option>';
    umdSelect.innerHTML = '<option value="">읍/면/동을 먼저 선택하세요</option>';

    const loadingOption = document.createElement('option');
    loadingOption.value = '';
    loadingOption.textContent = '시/도 데이터 로딩 중...';
    sidoSelect.innerHTML = '';
    sidoSelect.appendChild(loadingOption);

    try {
        const response = await fetch('/api/regions/sidos');
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        const sidos = await response.json();
        
        sidoSelect.innerHTML = '';
        
        if (sidos.length === 0) {
            const option = document.createElement('option');
            option.value = '';
            option.textContent = '시/도 데이터 없음';
            sidoSelect.appendChild(option);
        } else {
            const defaultOption = document.createElement('option');
            defaultOption.value = '';
            defaultOption.textContent = '시/도를 선택하세요';
            sidoSelect.appendChild(defaultOption);
            
            sidos.forEach(sido => {
                const option = document.createElement('option');
                option.value = sido;
                option.textContent = sido;
                sidoSelect.appendChild(option);
            });
        }
    } catch (error) {
        console.error('시/도 데이터 가져오기 실패:', error);
        sidoSelect.innerHTML = '';
        const option = document.createElement('option');
        option.value = '';
        option.textContent = '시/도 데이터를 불러올 수 없습니다';
        sidoSelect.appendChild(option);
    }
}

async function updateSggs() {
    const sido = document.getElementById('sido').value;
    const sggSelect = document.getElementById('sgg');
    const umdSelect = document.getElementById('umd');

    sggSelect.innerHTML = '<option value="">선택하세요</option>';
    umdSelect.innerHTML = '<option value="">먼저 시/군/구를 선택하세요</option>';

    if (!sido) {
        sggSelect.innerHTML = '<option value="">시/도를 먼저 선택하세요</option>';
        return;
    }

    try {
        console.log(`시/군/구 데이터 요청: ${sido}`);
        
        sggSelect.innerHTML = '<option value="">시/군/구 데이터 로딩 중...</option>';
        
        const response = await fetch(`/api/regions/sggs?sido=${encodeURIComponent(sido)}`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const sggs = await response.json();
        console.log(`시/군/구 데이터 응답:`, sggs);
        
        if (sggs.length === 0) {
            sggSelect.innerHTML = '<option value="">시/군/구 데이터 없음</option>';
        } else {
            sggSelect.innerHTML = '<option value="">선택하세요</option>';
            sggs.forEach(sgg => {
                const option = document.createElement('option');
                option.value = sgg;
                option.textContent = sgg;
                sggSelect.appendChild(option);
            });
        }
    } catch (error) {
        console.error('시/군/구 데이터 가져오기 실패:', error);
        sggSelect.innerHTML = '<option value="">시/군/구 데이터를 불러올 수 없습니다</option>';
    }
}

async function updateUmds() {
    const sido = document.getElementById('sido').value;
    const sgg = document.getElementById('sgg').value;
    const umdSelect = document.getElementById('umd');

    umdSelect.innerHTML = '<option value="">선택하세요</option>';

    if (!sido || !sgg) {
        if (!sido) {
            umdSelect.innerHTML = '<option value="">시/도를 먼저 선택하세요</option>';
        } else if (!sgg) {
            umdSelect.innerHTML = '<option value="">시/군/구를 먼저 선택하세요</option>';
        }
        return;
    }

    try {
        console.log(`읍/면/동 데이터 요청: ${sido}, ${sgg}`);
        
        umdSelect.innerHTML = '<option value="">읍/면/동 데이터 로딩 중...</option>';
        
        const response = await fetch(`/api/regions/umds?sido=${encodeURIComponent(sido)}&sgg=${encodeURIComponent(sgg)}`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const umds = await response.json();
        console.log(`읍/면/동 데이터 응답:`, umds);
        
        if (umds.length === 0) {
            umdSelect.innerHTML = '<option value="">읍/면/동 데이터 없음</option>';
        } else {
            umdSelect.innerHTML = '<option value="">선택하세요</option>';
            umds.forEach(umd => {
                const option = document.createElement('option');
                option.value = umd;
                option.textContent = umd;
                umdSelect.appendChild(option);
            });
        }
    } catch (error) {
        console.error('읍/면/동 데이터 가져오기 실패:', error);
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

    const confirmBtn = document.getElementById('confirmBtn');
    const originalText = confirmBtn.textContent;
    confirmBtn.textContent = '로딩 중...';
    confirmBtn.disabled = true;

    const locationTitle = document.getElementById('locationTitle');
    if (locationTitle) {
        locationTitle.textContent = `${umd} (${sgg})`;
    }

    fetch(`/weather?location=${encodeURIComponent(`${sido} ${sgg} ${umd}`)}`)
        .then(response => {
            if (response.ok) {
                closeLocationPopup();
                window.location.reload();
            } else {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
        })
        .catch(error => {
            console.error('날씨 데이터 업데이트 실패:', error);
            alert('날씨 데이터를 업데이트하는 데 실패했습니다.');
        })
        .finally(() => {
            confirmBtn.textContent = originalText;
            confirmBtn.disabled = false;
        });
}

function removeParentheses(text) {
    return text.replace(/\([^()]*\)/g, '').replace(/\[.*?\]/g, '').replace(/\./g, '').trim();
}

async function fetchChartData() {
    try {
        const response = await fetch('/api/chart/temperature');
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const data = await response.json();
        return data;
    } catch (error) {
        console.error('체감온도 차트 데이터 가져오기 실패:', error);
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

function createChart(button, startDate, temperatures) {
    const chartContainer = button.parentElement.querySelector('.chart-container');
    if (!chartContainer) {
        console.error('chartContainer를 찾을 수 없습니다.');
        return null;
    }

    const ctx = chartContainer.querySelector('#sentaTemperatureChart')?.getContext('2d');
    if (!ctx) {
        console.error('sentaTemperatureChart 캔버스를 찾을 수 없습니다.');
        return null;
    }

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

    if (chart) chart.destroy();
    chart = new Chart(ctx, {
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
                    title: { display: true, text: '시간 (KST)', font: { size: 16 } },
                    ticks: { maxTicksLimit: 12, autoSkip: true, font: { size: 12 } }
                },
                y: {
                    title: { display: true, text: '온도 (°C)', font: { size: 16 } },
                    beginAtZero: false,
                    suggestedMin: 14,
                    suggestedMax: 30,
                    ticks: { font: { size: 12 } }
                }
            },
            plugins: {
                legend: { display: true, position: 'top', labels: { font: { size: 14 } } },
                title: { display: true, text: '여름철 체감온도 예보 (2025년 6월 7일 기준)', font: { size: 20 } }
            }
        }
    });
    return chart;
}

async function toggleChart(button) {
    const chartContainer = button.parentElement.querySelector('.chart-container');
    if (!chartContainer) {
        console.error('chartContainer를 찾을 수 없습니다.');
        return;
    }

    button.classList.toggle('clicked');
    if (button.classList.contains('clicked')) {
        chartContainer.style.display = 'block';
        if (!chart) {
            try {
                const chartData = await fetchChartData();
                chart = createChart(button, chartData.startDate, chartData.temperatures);
                if (!chart) {
                    console.error('차트 생성 실패');
                    chartContainer.style.display = 'none';
                    button.classList.remove('clicked');
                }
            } catch (error) {
                console.error('체감온도 차트 생성 실패:', error);
                alert('차트를 생성하는 데 실패했습니다.');
                chartContainer.style.display = 'none';
                button.classList.remove('clicked');
            }
        }
    } else {
        chartContainer.style.display = 'none';
        if (chart) {
            chart.destroy();
            chart = null;
        }
    }
}

async function fetchAirStagnationChartData() {
    try {
        const response = await fetch('/api/airchart/air-stagnation');
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const data = await response.json();
        return data;
    } catch (error) {
        console.error('대기정체지수 차트 데이터 가져오기 실패:', error);
        return { startDate: getCurrentDateTimeFormatted(), indices: [50, 55, 60, 65, 70, 75, 80, 85, 90, 95] };
    }
}

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
    const ctx = document.getElementById('airStagnationChart')?.getContext('2d');
    if (!ctx) {
        console.error('airStagnationChart 캔버스를 찾을 수 없습니다.');
        return null;
    }
    if (airStagnationChart) airStagnationChart.destroy();
    airStagnationChart = new Chart(ctx, {
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
                x: { title: { display: true, text: '시간 (KST)', font: { size: 16 } }, ticks: { maxTicksLimit: 10, autoSkip: true, font: { size: 12 } } },
                y: { title: { display: true, text: '대기정체지수', font: { size: 16 } }, beginAtZero: false, suggestedMin: 40, suggestedMax: 110, ticks: { stepSize: 25, font: { size: 12 } } }
            },
            plugins: {
                legend: { display: true, position: 'top', labels: { font: { size: 14 } } },
                title: { display: true, text: '대기정체지수 예보 (2025년 6월 7일 기준)', font: { size: 20 } }
            }
        }
    });
    return airStagnationChart;
}

async function openAirStagnationChartPopup() {
    const popup = document.getElementById('airStagnationChartPopup');
    popup.style.display = 'flex';

    if (!airStagnationChart) {
        try {
            const data = await fetchAirStagnationChartData();
            airStagnationChart = createAirStagnationChart(data.startDate, data.indices);
            if (!airStagnationChart) closeAirStagnationChartPopup();
        } catch (error) {
            console.error('대기정체지수 차트 데이터 가져오기 실패:', error);
            alert('대기정체지수 차트를 불러올 수 없습니다.');
            closeAirStagnationChartPopup();
        }
    }
}

function closeAirStagnationChartPopup() {
    document.getElementById('airStagnationChartPopup').style.display = 'none';
    if (airStagnationChart) {
        airStagnationChart.destroy();
        airStagnationChart = null;
    }
}

async function fetchPrecipitationData() {
    try {
        const response = await fetch('/api/precipitation');
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const data = await response.json();
        return data;
    } catch (error) {
        console.error('강수량 데이터 가져오기 실패:', error);
        return { labels: ['6/7 0:00', '6/7 1:00', '6/7 2:00', '6/7 3:00', '6/7 4:00'], precipitations: [0.0, 0.5, 1.0, 0.5, 0.0] };
    }
}

function createPrecipitationChart(labels, precipitations) {
    const ctx = document.getElementById('precipitationChart')?.getContext('2d');
    if (!ctx) {
        console.error('precipitationChart 캔버스를 찾을 수 없습니다.');
        return null;
    }

    const allZero = precipitations.every(value => value === 0);
    const maxPrecipitation = Math.max(...precipitations, 1);

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
            maintainAspectRatio: false,
            scales: {
                x: { title: { display: true, text: '시간 (KST)', font: { size: 14 } } },
                y: { title: { display: true, text: '강수량 (mm)', font: { size: 14 } }, beginAtZero: true, max: allZero ? 1 : Math.ceil(maxPrecipitation * 1.2), ticks: { stepSize: allZero ? 0.2 : Math.ceil(maxPrecipitation * 1.2) / 5 } }
            },
            plugins: {
                legend: { display: true, position: 'top' },
                title: { display: true, text: allZero ? '강수량 예보 (2025년 6월 7일) - 강수 없음' : '강수량 예보 (2025년 6월 7일)', font: { size: 18 } }
            }
        }
    });
    return precipitationChart;
}

async function openPrecipitationChartPopup() {
    const popup = document.getElementById('precipitationChartPopup');
    popup.style.display = 'flex';
    const data = await fetchPrecipitationData();
    const newChart = createPrecipitationChart(data.labels, data.precipitations);
    if (!newChart) closePrecipitationChartPopup();
}

function closePrecipitationChartPopup() {
    document.getElementById('precipitationChartPopup').style.display = 'none';
    if (precipitationChart) {
        precipitationChart.destroy();
        precipitationChart = null;
    }
}

function getTempColor(temp) {
    if (temp >= 25) return '#e74c3c';
    if (temp >= 20) return '#f39c12';
    if (temp >= 15) return '#f1c40f';
    if (temp >= 10) return '#3498db';
    return '#9b59b6';
}

function getTempIcon(temp) {
    if (temp >= 30) return '🔥';
    if (temp >= 25) return '☀️';
    if (temp >= 20) return '🌤️';
    if (temp >= 15) return '⛅';
    if (temp >= 10) return '☁️';
    if (temp >= 5) return '🌥️';
    return '❄️';
}

function formatDate(dateStr) {
    const year = dateStr.substr(0, 4);
    const month = dateStr.substr(4, 2);
    const day = dateStr.substr(6, 2);
    const date = new Date(year, month - 1, day);
    const days = ['일', '월', '화', '수', '목', '금', '토'];
    return `${month}월 ${day}일 (${days[date.getDay()]})`;
}

function formatHourly(hour) {
    if (hour <= 12) return hour === 0 ? '12AM' : `${hour}AM`;
    return hour === 12 ? '12PM' : `${hour - 12}PM`;
}

async function openHourlyTemperaturePopup() {
    const popup = document.getElementById('hourlyTemperaturePopup');
    popup.style.display = 'flex';

    try {
        const response = await fetch('/api/hourly-temperature');
        if (!response.ok) throw new Error('시간별 온도 데이터를 가져오지 못했습니다.');
        const data = await response.json();

        displayTemperatureChart(data);
        displayHourlyData(data);
    } catch (error) {
        console.error('시간별 온도 데이터 로드 실패:', error);
        alert('시간별 온도 데이터를 로드하는 데 실패했습니다.');
        closeHourlyTemperaturePopup();
    }
}

function closeHourlyTemperaturePopup() {
    document.getElementById('hourlyTemperaturePopup').style.display = 'none';
    if (temperatureChart) {
        temperatureChart.destroy();
        temperatureChart = null;
    }
}

function displayTemperatureChart(data) {
    const ctx = document.getElementById('temperatureChart').getContext('2d');

    if (temperatureChart) temperatureChart.destroy();

    const labels = [];
    const temperatures = [];
    const backgroundColors = [];

    const baseDate = new Date(data.date.substr(0, 4), data.date.substr(4, 2) - 1, data.date.substr(6, 2));

    for (let i = 1; i <= 72; i++) {
        const temp = data.temperatures[`h${i}`];
        if (temp && temp !== '') {
            const tempValue = parseFloat(temp);
            const currentDate = new Date(baseDate);
            currentDate.setHours(currentDate.getHours() + i);

            labels.push(i <= 24 ? `${i}시` : `${Math.floor((i - 1) / 24) + 1}일차 ${((i - 1) % 24) + 1}시`);
            temperatures.push(tempValue);
            backgroundColors.push(getTempColor(tempValue));
        }
    }

    temperatureChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: '온도 (°C)',
                data: temperatures,
                borderColor: '#667eea',
                backgroundColor: 'rgba(102, 126, 234, 0.1)',
                borderWidth: 3,
                fill: true,
                tension: 0.4,
                pointBackgroundColor: backgroundColors,
                pointBorderColor: backgroundColors,
                pointRadius: 4,
                pointHoverRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false }, tooltip: { callbacks: { label: context => `온도: ${context.parsed.y}°C` } } },
            scales: { y: { beginAtZero: false, grid: { color: 'rgba(0,0,0,0.1)' }, ticks: { callback: value => value + '°C' } }, x: { grid: { color: 'rgba(0,0,0,0.1)' } } }
        }
    });
}

function displayHourlyData(data) {
    const hourlyContainer = document.getElementById('hourlyData');
    const baseDate = new Date(data.date.substr(0, 4), data.date.substr(4, 2) - 1, data.date.substr(6, 2));

    let daysHTML = '';
    let currentDayHTML = '';
    let currentDay = '';
    let hourCardsHTML = '';

    for (let i = 1; i <= 72; i++) {
        const temp = data.temperatures[`h${i}`];
        if (temp && temp !== '') {
            const tempValue = parseFloat(temp);
            const currentDate = new Date(baseDate);
            currentDate.setHours(currentDate.getHours() + i);

            const dayStr = formatDate(
                currentDate.getFullYear().toString() +
                (currentDate.getMonth() + 1).toString().padStart(2, '0') +
                currentDate.getDate().toString().padStart(2, '0')
            );

            if (dayStr !== currentDay) {
                if (currentDay !== '') {
                    currentDayHTML += `<div class="day-content"><div class="hourly-grid">${hourCardsHTML}</div></div>`;
                    daysHTML += currentDayHTML;
                }
                currentDay = dayStr;
                currentDayHTML = `<div class="day-section"><div class="day-header">${dayStr}</div>`;
                hourCardsHTML = '';
            }

            const hour = currentDate.getHours();
            const isCurrentHour = i === 1;

            hourCardsHTML += `
                <div class="hour-card ${isCurrentHour ? 'current' : ''}">
                    <div class="hour-time">${formatHourly(hour)}</div>
                    <div class="hour-temp" style="color: ${isCurrentHour ? 'white' : getTempColor(tempValue)}">
                        ${getTempIcon(tempValue)} ${tempValue}°C
                    </div>
                </div>
            `;
        }
    }

    if (currentDay !== '') {
        currentDayHTML += `<div class="day-content"><div class="hourly-grid">${hourCardsHTML}</div></div></div>`;
        daysHTML += currentDayHTML;
    }

    hourlyContainer.innerHTML = daysHTML;
}

async function fetchWeatherData(nx, ny) {
    try {
        const response = await fetch(`/api/weather?nx=${nx}&ny=${ny}`);
        if (!response.ok) throw new Error('날씨 데이터 가져오기 실패');
        return await response.json();
    } catch (error) {
        console.error('날씨 데이터 요청 오류:', error);
        return null;
    }
}

async function fetchHourlyTemperature(areaNo, time) {
    try {
        const response = await fetch(`/api/hourly-temperature?areaNo=${areaNo}&time=${time}`);
        if (!response.ok) throw new Error('시간별 온도 데이터 가져오기 실패');
        return await response.json();
    } catch (error) {
        console.error('시간별 온도 데이터 요청 오류:', error);
        return { date: '', temperatures: {} };
    }
}

async function openWeatherDetailPopup() {
    let popup = document.getElementById('weatherDetailPopup');
    if (!popup) {
        const popupHtml = `
            <div id="weatherDetailPopup" class="popup" role="dialog" aria-label="날씨 상세 정보 팝업">
                <div class="popup-content">
                    <button class="close-btn" onclick="closeWeatherDetailPopup()" aria-label="팝업 닫기">X</button>
                    <div class="weather-detail-container"></div>
                </div>
            </div>
        `;
        document.body.insertAdjacentHTML('beforeend', popupHtml);
        popup = document.getElementById('weatherDetailPopup');
    }

    popup.style.display = 'flex';

    const nx = 60;
    const ny = 127;

    const weatherData = await fetchWeatherData(nx, ny);
    const hourlyData = await fetchHourlyTemperature('A41', 'now');

    const container = document.querySelector('#weatherDetailPopup .weather-detail-container');
    if (weatherData?.response?.body?.items && hourlyData?.temperatures) {
        const items = weatherData.response.body.items;
        const hourlyItems = hourlyData.temperatures;

        container.innerHTML = `
            <div class="weather-main">
                <div class="current-temp">
                    <span class="temp-value">${items.find(i => i.category === 'TMP')?.value || 14}°C</span>
                    <div class="temp-range">최저 ${items.find(i => i.category === 'TMN')?.value || -2}°C / 최고 ${items.find(i => i.category === 'TMX')?.value || 2}°C</div>
                </div>
                <div class="weather-info">
                    <div class="weather-condition">${items.find(i => i.category === 'SKY')?.value || '맑음'}</div>
                    <div class="humidity">${items.find(i => i.category === 'REH')?.value || 50}%</div>
                    <div class="pm-value">풍속 ${items.find(i => i.category === 'WSD')?.value || 3.1}m/s</div>
                </div>
            </div>
            <div class="weather-details">
                <table>
                    <tr><th>예보시간</th><th>항목</th><th>값</th><th>단위</th></tr>
                    ${Object.entries(hourlyItems).map(([key, value]) => `
                        <tr>
                            <td>${key.replace('h', '')}:00</td>
                            <td>TMP</td>
                            <td>${value}</td>
                            <td>°C</td>
                        </tr>
                    `).join('')}
                </table>
            </div>
        `;
    } else {
        container.innerHTML = '<p>데이터를 불러올 수 없습니다.</p>';
    }
}

function closeWeatherDetailPopup() {
    const popup = document.getElementById('weatherDetailPopup');
    if (popup) popup.style.display = 'none';
}

async function showWeatherStats() {
    const popup = document.getElementById('weatherStatsPopup');
    popup.style.display = 'flex';
    await loadWeatherStats();
}

function closeWeatherStatsPopup() {
    const popup = document.getElementById('weatherStatsPopup');
    popup.style.display = 'none';
    if (dailyChart) dailyChart.destroy();
    if (threeDayChart) threeDayChart.destroy();
}

async function loadWeatherStats() {
    try {
        const response = await fetch('/api/hourly-temperature');
        if (!response.ok) throw new Error('데이터 로드 실패');
        weatherData = await response.json();
        const { hourlyData, days } = parseWeatherData(weatherData);
        updateCurrentTime();
        setInterval(updateCurrentTime, 1000);
        setupDayTabs(days);
        updateDashboard(days);
    } catch (error) {
        console.error('Error:', error);
        alert('날씨 데이터를 불러오지 못했습니다.');
        closeWeatherStatsPopup();
    }
}

function updateCurrentTime() {
    const currentTime = document.getElementById('currentTime');
    currentTime.textContent = new Date().toLocaleString('ko-KR');
}

function parseWeatherData(data) {
    let baseDate = new Date();
    let hourlyData = [];
    let days = [[], [], []];

    if (data.date) {
        baseDate = new Date(data.date.substr(0, 4), data.date.substr(4, 2) - 1, data.date.substr(6, 2));
    }

    if (data.temperatures) {
        for (let i = 1; i <= 72; i++) {
            const temp = data.temperatures[`h${i}`];
            if (temp && temp !== '') {
                const hour = (i - 1) % 24;
                const day = Math.floor((i - 1) / 24);
                const currentDate = new Date(baseDate);
                currentDate.setDate(baseDate.getDate() + day);
                currentDate.setHours(hour, 0, 0, 0);

                const dataPoint = {
                    hour: hour,
                    time: `${hour.toString().padStart(2, '0')}:00`,
                    temp: parseInt(temp),
                    fullTime: currentDate,
                    day: day
                };

                hourlyData.push(dataPoint);
                if (day < 3) days[day].push(dataPoint);
            }
        }
    } else {
        console.warn('No temperatures data found in response:', data);
    }

    return { hourlyData, days };
}

function setupDayTabs(days) {
    const tabs = document.querySelectorAll('.day-tab');
    tabs.forEach((tab, index) => {
        tab.addEventListener('click', () => {
            selectedDay = index;
            tabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            updateDashboard(days);
        });
        if (index === 0) tab.classList.add('active');
    });
}

function updateDashboard(days) {
    const dayData = days[selectedDay];
    updateStats(dayData);
    updateDailyChart(dayData);
    updateThreeDayChart(days.flat());
    updateHourlyDetails(dayData);
}

function updateStats(dayData) {
    const stats = getDayStats(dayData);
    const trend = getTempTrend(dayData);
    document.getElementById('minTemp').textContent = `${stats.min}°C`;
    document.getElementById('maxTemp').textContent = `${stats.max}°C`;
    document.getElementById('avgTemp').textContent = `${stats.avg}°C`;
    document.getElementById('trendValue').textContent = trend === 'up' ? '상승' : trend === 'down' ? '하강' : '안정';
    document.getElementById('trendIcon').textContent = trend === 'up' ? '🔺' : trend === 'down' ? '🔻' : '➖';
}

function getDayStats(dayData) {
    if (!dayData.length) return { min: 0, max: 0, avg: 0 };
    const temps = dayData.map(d => d.temp);
    return { min: Math.min(...temps), max: Math.max(...temps), avg: Math.round(temps.reduce((a, b) => a + b, 0) / temps.length) };
}

function getTempTrend(dayData) {
    if (dayData.length < 2) return 'stable';
    const first = dayData[0].temp;
    const last = dayData[dayData.length - 1].temp;
    return last > first + 2 ? 'up' : last < first - 2 ? 'down' : 'stable';
}

function updateDailyChart(dayData) {
    const ctx = document.getElementById('dailyChart').getContext('2d');
    if (dailyChart) dailyChart.destroy();
    dailyChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: dayData.map(d => d.time),
            datasets: [{
                label: '체감 온도 (°C)',
                data: dayData.map(d => d.temp),
                borderColor: '#3b82f6',
                backgroundColor: 'rgba(59, 130, 246, 0.1)',
                borderWidth: 3,
                fill: true,
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: { x: { title: { display: true, text: '시간' } }, y: { title: { display: true, text: '온도 (°C)' } } },
            plugins: { legend: { display: false } }
        }
    });
}

function updateThreeDayChart(hourlyData) {
    const ctx = document.getElementById('threeDayChart').getContext('2d');
    if (threeDayChart) threeDayChart.destroy();
    threeDayChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: hourlyData.map(d => `${d.day + 1}일 ${d.time}`),
            datasets: [{
                label: '체감 온도 (°C)',
                data: hourlyData.map(d => d.temp),
                borderColor: '#8b5cf6',
                backgroundColor: 'rgba(139, 92, 246, 0.2)',
                borderWidth: 2,
                fill: true,
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: { x: { title: { display: true, text: '시간' }, ticks: { maxTicksLimit: 12 } }, y: { title: { display: true, text: '온도 (°C)' } } },
            plugins: { legend: { display: false } }
        }
    });
}

function updateHourlyDetails(dayData) {
    const container = document.getElementById('hourlyDetails');
    container.innerHTML = dayData.map(data => `
        <div class="hour-card">
            <div class="hour-icon">${getWeatherIcon(data.temp)}</div>
            <p class="hour-time">${data.time}</p>
            <p class="hour-temp">${data.temp}°C</p>
            <p class="hour-desc">${getTempDescription(data.temp)}</p>
        </div>
    `).join('');
}

function getWeatherIcon(temp) {
    if (temp >= 25) return '☀️';
    if (temp >= 20) return '⛅';
    return '🌧️';
}

function getTempDescription(temp) {
    if (temp >= 25) return '덥다';
    if (temp >= 20) return '따뜻';
    if (temp >= 15) return '선선';
    return '쌀쌀';
}

document.addEventListener('DOMContentLoaded', () => {
    const overallTexts = document.querySelectorAll('.overall-text');
    overallTexts.forEach(element => element.textContent = removeParentheses(element.textContent));
    const causeTexts = document.querySelectorAll('.cause-text');
    causeTexts.forEach(element => element.textContent = removeParentheses(element.textContent));

    const locationTitle = document.getElementById('locationTitle');
    if (!locationTitle.textContent.trim()) locationTitle.textContent = '청진동 (종로구)';

    const sidoSelect = document.getElementById('sido');
    const selectedSido = '';
    if (selectedSido && selectedSido !== '') {
        sidoSelect.value = selectedSido;
        updateSggs().then(() => {
            const sggSelect = document.getElementById('sgg');
            const selectedSgg = '';
            if (selectedSgg && selectedSgg !== '') {
                sggSelect.value = selectedSgg;
                updateUmds().then(() => {
                    const umdSelect = document.getElementById('umd');
                    const selectedUmd = '';
                    if (selectedUmd && selectedUmd !== '') umdSelect.value = selectedUmd;
                });
            }
        });
    }

    const extraButton = document.querySelector('.dust-buttons-container .dust-forecast-btn:first-child');
    if (extraButton) extraButton.addEventListener('click', () => alert('안녕 디지몬'));

    const pm25Value = document.querySelector('.pm25-value');
    if (pm25Value) pm25Value.innerHTML = '<button class="unified-btn" onclick="openWeatherDetailPopup()">날씨정보상세보기</button>';
});