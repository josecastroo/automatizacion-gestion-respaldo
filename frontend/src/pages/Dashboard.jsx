import { useEffect, useState } from 'react';
import { AlertCircle, Info, CheckCircle2 } from 'lucide-react';

export default function Dashboard() {
    const [alerts, setAlerts] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // Llama al backend de Spring Boot (asegúrate de tener CorsConfig habilitado en Java)
        fetch('http://localhost:8080/api/alerts')
            .then((res) => res.json())
            .then((data) => {
                setAlerts(data);
                setLoading(false);
            })
            .catch((err) => {
                console.error("Error al cargar alertas:", err);
                setLoading(false);
            });
    }, []);

    const getAlertStyle = (level) => {
        switch (level) {
            case 'ADVERTENCIA':
                return { bg: 'bg-orange-50', border: 'border-orange-100', text: 'text-orange-800', icon: <AlertCircle className="text-orange-500" size={24} /> };
            case 'RECOMENDACION':
                return { bg: 'bg-blue-50', border: 'border-blue-100', text: 'text-blue-800', icon: <Info className="text-blue-500" size={24} /> };
            default:
                return { bg: 'bg-gray-50', border: 'border-gray-200', text: 'text-gray-800', icon: <Info className="text-gray-500" size={24} /> };
        }
    };

    return (
        <div className="max-w-5xl mx-auto space-y-8 animate-in fade-in duration-500">
            <div>
                <h2 className="text-3xl font-semibold tracking-tight text-gray-900">Resumen del Sistema</h2>
                <p className="text-gray-500 mt-2">Monitoreo preventivo de estrategias y ejecución RMAN.</p>
            </div>

            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
                <h3 className="text-lg font-medium text-gray-900 mb-6">Alertas y Recomendaciones</h3>

                {loading ? (
                    <div className="flex justify-center py-8">
                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                    </div>
                ) : alerts.length === 0 ? (
                    <div className="flex flex-col items-center justify-center py-12 text-center bg-green-50 rounded-xl border border-green-100">
                        <CheckCircle2 className="text-green-500 mb-3" size={32} />
                        <p className="text-green-800 font-medium">Todo está en orden</p>
                        <p className="text-green-600 text-sm mt-1">No hay alertas ni riesgos de integridad detectados.</p>
                    </div>
                ) : (
                    <div className="space-y-4">
                        {alerts.map((alert, index) => {
                            const style = getAlertStyle(alert.level);
                            return (
                                <div key={index} className={`flex items-start gap-4 p-5 rounded-xl border ${style.bg} ${style.border} transition-all hover:shadow-md`}>
                                    <div className="mt-0.5">{style.icon}</div>
                                    <div>
                                        <h4 className={`font-semibold text-sm tracking-wide uppercase mb-1 ${style.text}`}>
                                            {alert.level}
                                        </h4>
                                        <p className="text-gray-700 leading-relaxed">
                                            {alert.message}
                                        </p>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>
        </div>
    );
}