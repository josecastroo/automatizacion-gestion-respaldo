import { useEffect, useState } from 'react';
import { ShieldAlert, Info, CheckCircle2, RefreshCw, Server, Shield, Database, Activity, ArrowUpRight } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function Dashboard() {
    const [alerts, setAlerts] = useState([]);
    const [databases, setDatabases] = useState([]);
    const [strategies, setStrategies] = useState([]);
    const [loading, setLoading] = useState(true);

    const fetchData = () => {
        setLoading(true);
        Promise.all([
            fetch('http://localhost:8080/api/alerts').then(res => res.json()).catch(() => []),
            fetch('http://localhost:8080/api/databases').then(res => res.json()).catch(() => []),
            fetch('http://localhost:8080/api/strategies').then(res => res.json()).catch(() => [])
        ])
            .then(([alertsData, dbData, stratData]) => {
                setAlerts(alertsData);
                setDatabases(dbData);
                setStrategies(stratData);
                setLoading(false);
            });
    };

    useEffect(() => {
        fetchData();
    }, []);

    const warnings = alerts.filter(a => a.level === 'ADVERTENCIA');
    const recommendations = alerts.filter(a => a.level === 'RECOMENDACION');
    const activeStrategies = strategies.filter(s => s.active);

    return (
        <div className="max-w-6xl mx-auto space-y-8 animate-in fade-in duration-500 pb-12">

            {/* Header Estilo Banner Minimalista */}
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-gradient-to-br from-gray-900 to-gray-800 text-white p-8 rounded-3xl shadow-lg relative overflow-hidden">
                <div className="absolute right-0 top-0 translate-x-8 -translate-y-8 w-64 h-64 bg-blue-500/10 rounded-full blur-3xl pointer-events-none"></div>
                <div className="relative z-10 space-y-1">
                    <span className="text-xs font-semibold tracking-wider text-blue-400 uppercase">Sistema Activo</span>
                    <h2 className="text-3xl font-bold tracking-tight">RMAN Manager Pro</h2>
                    <p className="text-gray-400 text-sm max-w-lg">Control centralizado de respaldos para bases de datos Oracle. Todo opera bajo parámetros seguros.</p>
                </div>
                <button
                    onClick={fetchData}
                    className="relative z-10 flex items-center gap-2 bg-white/10 hover:bg-white/20 text-white px-4 py-2.5 rounded-xl font-medium transition-all backdrop-blur-md border border-white/10 text-sm"
                >
                    <RefreshCw size={16} className={loading ? "animate-spin text-blue-400" : ""} />
                    Sincronizar
                </button>
            </div>

            {/* Grid de Estadísticas Principales */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
                <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm flex flex-col justify-between">
                    <div className="flex justify-between items-start">
                        <div className="p-3 bg-blue-50 text-blue-600 rounded-xl">
                            <Database size={22} />
                        </div>
                        <Link to="/databases" className="text-gray-400 hover:text-blue-600 transition-colors">
                            <ArrowUpRight size={18} />
                        </Link>
                    </div>
                    <div className="mt-4">
                        <h3 className="text-3xl font-bold text-gray-900">{databases.length}</h3>
                        <p className="text-sm font-medium text-gray-500 mt-0.5">Bases Registradas</p>
                    </div>
                </div>

                <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm flex flex-col justify-between">
                    <div className="flex justify-between items-start">
                        <div className="p-3 bg-indigo-50 text-indigo-600 rounded-xl">
                            <Shield size={22} />
                        </div>
                        <Link to="/strategies" className="text-gray-400 hover:text-indigo-600 transition-colors">
                            <ArrowUpRight size={18} />
                        </Link>
                    </div>
                    <div className="mt-4">
                        <h3 className="text-3xl font-bold text-gray-900">{activeStrategies.length} <span className="text-sm font-normal text-gray-400">/ {strategies.length}</span></h3>
                        <p className="text-sm font-medium text-gray-500 mt-0.5">Estrategias Activas</p>
                    </div>
                </div>

                <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm flex flex-col justify-between">
                    <div className="flex justify-between items-start">
                        <div className="p-3 bg-amber-50 text-amber-600 rounded-xl">
                            <ShieldAlert size={22} />
                        </div>
                    </div>
                    <div className="mt-4">
                        <h3 className="text-3xl font-bold text-amber-600">{warnings.length}</h3>
                        <p className="text-sm font-medium text-gray-500 mt-0.5">Advertencias Críticas</p>
                    </div>
                </div>

                <div className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm flex flex-col justify-between">
                    <div className="flex justify-between items-start">
                        <div className="p-3 bg-emerald-50 text-emerald-600 rounded-xl">
                            <Activity size={22} />
                        </div>
                    </div>
                    <div className="mt-4">
                        <h3 className="text-3xl font-bold text-emerald-600">{recommendations.length}</h3>
                        <p className="text-sm font-medium text-gray-500 mt-0.5">Sugerencias DB</p>
                    </div>
                </div>
            </div>

            {/* Sección Inferior: Alertas Detalladas y Accesos Rápidos */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">

                {/* Columna Izquierda/Central: Alertas en Tarjetas Modernas (Ocupa 2 espacios) */}
                <div className="lg:col-span-2 bg-white rounded-3xl shadow-sm border border-gray-100 p-8 space-y-6">
                    <div className="flex justify-between items-center">
                        <div>
                            <h3 className="text-lg font-bold text-gray-900">Estado de Integridad y Alertas</h3>
                            <p className="text-xs text-gray-400 mt-0.5">Diagnóstico en vivo devuelto por el motor Java.</p>
                        </div>
                        <span className="px-3 py-1 bg-gray-100 text-gray-600 rounded-full text-xs font-semibold">
              {alerts.length} eventos
            </span>
                    </div>

                    {loading ? (
                        <div className="flex justify-center py-20">
                            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                        </div>
                    ) : alerts.length === 0 ? (
                        <div className="flex flex-col items-center justify-center py-16 text-center bg-emerald-50/50 rounded-2xl border border-emerald-100/60">
                            <div className="p-3 bg-emerald-100 text-emerald-600 rounded-full mb-3">
                                <CheckCircle2 size={32} />
                            </div>
                            <p className="text-emerald-900 font-semibold text-lg">Sin anomalías</p>
                            <p className="text-emerald-700 text-sm mt-1 max-w-xs">Todas las bases de datos y estrategias operan bajo los estándares óptimos.</p>
                        </div>
                    ) : (
                        <div className="space-y-3">
                            {alerts.map((alert, index) => {
                                const isWarning = alert.level === 'ADVERTENCIA';
                                return (
                                    <div key={index} className={`p-4 rounded-2xl border flex items-start gap-4 transition-all ${
                                        isWarning ? 'bg-amber-50/40 border-amber-100' : 'bg-blue-50/40 border-blue-100'
                                    }`}>
                                        <div className={`p-2 rounded-xl mt-0.5 ${isWarning ? 'bg-amber-100 text-amber-700' : 'bg-blue-100 text-blue-700'}`}>
                                            {isWarning ? <ShieldAlert size={18} /> : <Info size={18} />}
                                        </div>
                                        <div className="flex-1">
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded uppercase tracking-wider ${
                          isWarning ? 'bg-amber-200/60 text-amber-900' : 'bg-blue-200/60 text-blue-900'
                      }`}>
                        {alert.level}
                      </span>
                                            <p className="text-gray-700 text-sm mt-1.5 font-medium leading-relaxed">
                                                {alert.message}
                                            </p>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>

                {/* Columna Derecha: Panel de Acciones Rápidas */}
                <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-8 flex flex-col justify-between">
                    <div className="space-y-6">
                        <div>
                            <h3 className="text-lg font-bold text-gray-900">Accesos Directos</h3>
                            <p className="text-xs text-gray-400 mt-0.5">Gestión rápida del sistema.</p>
                        </div>

                        <div className="space-y-3">
                            <Link to="/databases" className="flex items-center justify-between p-4 rounded-2xl bg-gray-50 hover:bg-blue-50/60 text-gray-700 hover:text-blue-700 border border-gray-100 transition-all group">
                                <div className="flex items-center gap-3">
                                    <div className="p-2 bg-white rounded-xl shadow-xs text-blue-600 group-hover:scale-110 transition-transform">
                                        <Database size={18} />
                                    </div>
                                    <span className="font-semibold text-sm">Gestionar Bases de Datos</span>
                                </div>
                                <ArrowUpRight size={16} className="text-gray-400 group-hover:text-blue-600" />
                            </Link>

                            <Link to="/strategies" className="flex items-center justify-between p-4 rounded-2xl bg-gray-50 hover:bg-indigo-50/60 text-gray-700 hover:text-indigo-700 border border-gray-100 transition-all group">
                                <div className="flex items-center gap-3">
                                    <div className="p-2 bg-white rounded-xl shadow-xs text-indigo-600 group-hover:scale-110 transition-transform">
                                        <Shield size={18} />
                                    </div>
                                    <span className="font-semibold text-sm">Configurar Estrategias</span>
                                </div>
                                <ArrowUpRight size={16} className="text-gray-400 group-hover:text-indigo-600" />
                            </Link>
                        </div>
                    </div>

                    <div className="mt-8 pt-6 border-t border-gray-100">
                        <div className="bg-gradient-to-r from-blue-50 to-indigo-50 p-4 rounded-2xl border border-blue-100/60">
                            <p className="text-xs font-semibold text-blue-900">Consejo Pro</p>
                            <p className="text-xs text-blue-700 mt-1">Revisa el historial regularmente para validar que las ejecuciones programadas concluyan sin errores.</p>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    );
}