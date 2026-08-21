(function(){
  'use strict';
  const O = window.Oyuki;
  if (!O) return;
  const status = document.getElementById('homeMarketLocationStatus');
  const grid = document.getElementById('homeNearbyMarkets');
  const button = document.getElementById('homeUseLocation');
  if (!status || !grid) return;

  const esc = v => O.escapeHtml(v == null ? '' : String(v));

  function render(result){
    if (!result?.lgaId){
      status.innerHTML = `<i class="bi bi-geo-alt"></i> ${esc(result?.message || 'Choose your LGA from Market Square.')}`;
      grid.innerHTML = '<div class="col-12"><a class="btn btn-outline-brand" href="markets.html">Select State & LGA</a></div>';
      return;
    }
    status.innerHTML = `<i class="bi bi-geo-alt-fill"></i> Markets in <strong>${esc(result.detectedLga)}</strong>${result.detectedState ? `, ${esc(result.detectedState)}` : ''}`;
    const markets = Array.isArray(result.markets) ? result.markets.slice(0,6) : [];
    if (!markets.length){
      grid.innerHTML = `<div class="col-12"><div class="market-home-empty">No active Oyuki markets have been added for ${esc(result.detectedLga)} yet. <a href="markets.html">Browse Market Square</a></div></div>`;
      return;
    }
    grid.innerHTML = markets.map(m => `<div class="col-6 col-md-4 col-lg-2"><a class="home-market-card" href="shop.html?marketId=${encodeURIComponent(m.id)}"><span class="home-market-icon"><i class="bi bi-shop"></i></span><strong>${esc(m.name)}</strong><small>${m.distanceKm != null ? `${esc(m.distanceKm)} km away` : esc(result.detectedLga)}</small></a></div>`).join('');
  }

  async function resolve(position){
    status.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Finding markets in your LGA…';
    try{
      const {latitude,longitude} = position.coords;
      const result = O.unwrap(await O.Api.get(`/market-directory/nearby?lat=${encodeURIComponent(latitude)}&lng=${encodeURIComponent(longitude)}`, false));
      render(result);
      if (result?.lgaId){
        sessionStorage.setItem('oyuki_detected_market_area', JSON.stringify({stateId:result.stateId,lgaId:result.lgaId,lga:result.detectedLga,state:result.detectedState,lat:latitude,lng:longitude}));
      }
    }catch(error){
      status.innerHTML = `<i class="bi bi-exclamation-circle"></i> ${esc(error.message || 'Unable to detect nearby markets.')}`;
      grid.innerHTML = '<div class="col-12"><a class="btn btn-outline-brand" href="markets.html">Choose your LGA manually</a></div>';
    }
  }

  function request(){
    if (!navigator.geolocation){
      status.textContent = 'Location is not supported by this browser.';
      grid.innerHTML = '<div class="col-12"><a class="btn btn-outline-brand" href="markets.html">Choose your LGA manually</a></div>';
      return;
    }
    navigator.geolocation.getCurrentPosition(resolve, () => {
      status.innerHTML = '<i class="bi bi-geo-alt"></i> Location access was not granted. You can select your LGA manually.';
      grid.innerHTML = '<div class="col-12"><a class="btn btn-outline-brand" href="markets.html">Select State & LGA</a></div>';
    }, {enableHighAccuracy:true, timeout:12000, maximumAge:300000});
  }

  button?.addEventListener('click', request);

  document.addEventListener('DOMContentLoaded', () => {
    const cached = sessionStorage.getItem('oyuki_detected_market_area');
    if (cached){
      try{
        const area = JSON.parse(cached);
        if (area.lat != null && area.lng != null){
          resolve({coords:{latitude:area.lat,longitude:area.lng}});
          return;
        }
      }catch{}
    }
    request();
  });
})();
