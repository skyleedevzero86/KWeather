class SplashScreen {
  constructor() {
    this.tapCount = 0;
    this.isAnimating = false;
    this.messageIndex = 0;
    this.iconIndex = 0;
    
    this.splashIcon = document.getElementById("splashIcon");
    this.splashText = document.getElementById("splashText");
    this.tapHint = document.getElementById("tapHint");

    if (!this.splashIcon || !this.splashText || !this.tapHint) {
      console.error("스플래시 화면 요소를 찾을 수 없습니다");
      setTimeout(() => {
        window.location.href = "/weather";
      }, 2000);
      return;
    }

    this.weatherIcons = [
      "🌤️",
      "☀️",
      "⛅",
      "🌥️",
      "☁️",
      "🌦️",
      "⛈️",
      "🌧️",
      "❄️",
      "🌨️",
    ];
    
    this.loadingMessages = [
      "날씨 정보를 불러오는 중...",
      "위치 정보를 확인하는 중...",
      "실시간 데이터를 수집하는 중...",
      "예보를 분석하는 중...",
      "거의 완료되었습니다...",
    ];

    this.init();
  }

  init() {
    if (!this.splashIcon || !this.splashText || !this.tapHint) {
      return;
    }
    this.setupEventListeners();
    this.startAnimations();
    this.setupTimeouts();
  }

  setupEventListeners() {
    this.splashIcon.addEventListener("click", (e) => this.handleIconClick(e));
    this.splashIcon.addEventListener("mouseenter", () => this.handleMouseEnter());
    this.splashIcon.addEventListener("mouseleave", () => this.handleMouseLeave());
  }

  startAnimations() {
    setInterval(() => this.changeWeatherIcon(), 2000);
    setInterval(() => this.changeLoadingMessage(), 600);
  }

  setupTimeouts() {
    setTimeout(() => {
      this.tapHint.style.display = "none";
    }, 5000);

    setTimeout(() => {
      if (this.tapCount === 0) {
        this.splashText.textContent = "아이콘을 클릭해보세요!";
        this.splashText.style.letterSpacing = "0.5px";
        this.splashText.style.wordSpacing = "1px";
        this.tapHint.style.display = "block";
        this.tapHint.style.animation = "tapHint 1s infinite";
      }
    }, 2000);

    setTimeout(() => {
      if (this.tapCount === 0) {
        window.location.href = "/weather";
      }
    }, 10000);
  }

  createSparkle(x, y) {
    const sparkle = document.createElement("div");
    sparkle.className = "sparkle";
    sparkle.style.left = x + "px";
    sparkle.style.top = y + "px";
    document.body.appendChild(sparkle);

    setTimeout(() => {
      sparkle.remove();
    }, 1000);
  }

  changeWeatherIcon() {
    this.iconIndex = (this.iconIndex + 1) % this.weatherIcons.length;
    this.splashIcon.textContent = this.weatherIcons[this.iconIndex];
  }

  changeLoadingMessage() {
    if (this.messageIndex < this.loadingMessages.length) {
      this.splashText.textContent = this.loadingMessages[this.messageIndex];
      this.splashText.style.letterSpacing = "0.3px";
      this.splashText.style.wordSpacing = "0.5px";
      this.messageIndex++;
    }
  }

  addShakeEffect() {
    this.splashIcon.classList.add("shake");
    setTimeout(() => {
      this.splashIcon.classList.remove("shake");
    }, 500);
  }

  handleIconClick(e) {
    if (this.isAnimating) return;

    this.isAnimating = true;
    this.tapCount++;

    this.createSparkle(e.clientX, e.clientY);
    this.addShakeEffect();
    this.changeWeatherIcon();

    this.handleTapCountEffects();

    setTimeout(() => {
      this.isAnimating = false;
    }, 300);
  }

  handleTapCountEffects() {
    if (this.tapCount === 3) {
      this.splashText.textContent = "와! 날씨 마법사! 🌟";
      this.splashIcon.style.animation = "none";
      this.splashIcon.style.transform = "scale(1.2)";
      this.splashIcon.style.filter =
        "drop-shadow(0 0 20px rgba(255, 255, 255, 0.8))";
    } else if (this.tapCount === 5) {
      this.splashText.textContent = "이제 진짜 날씨를 보러 가볼까요? ✨";
      this.splashIcon.style.animation = "bounce 0.5s infinite";
    } else if (this.tapCount >= 7) {
      this.splashText.textContent = "좋아요! 바로 이동합니다! 🚀";
      setTimeout(() => {
        window.location.href = "/weather";
      }, 1000);
      return;
    }
  }

  handleMouseEnter() {
    if (!this.isAnimating) {
      this.splashIcon.style.transform = "scale(1.1)";
    }
  }

  handleMouseLeave() {
    if (!this.isAnimating && this.tapCount < 3) {
      this.splashIcon.style.transform = "scale(1)";
    }
  }
}

document.addEventListener("DOMContentLoaded", () => {
  console.log("DOM이 로드되었습니다, 스플래시 화면을 초기화합니다...");
  console.log("스플래시 아이콘 요소:", document.getElementById("splashIcon"));
  console.log("스플래시 텍스트 요소:", document.getElementById("splashText"));
  console.log("탭 힌트 요소:", document.getElementById("tapHint"));
  new SplashScreen();
});