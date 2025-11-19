import React from 'react';
import MapView from '../components/MapView';
import '../styles/App.css';

export default function MapPage() {
  return (
    <main style={{ padding: 16 }}>
      <h2 style={{ marginTop: 8 }}>Carte en temps réel</h2>
      <p style={{ color: '#6b7280' }}>Visualisation des bus en circulation (position et horaire prévu).</p>
      <div style={{ marginTop: 12 }}>
        <MapView />
      </div>
    </main>
  );
}
