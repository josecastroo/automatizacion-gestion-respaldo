import { useEffect, useState } from 'react';
import { Download, FileText, CheckCircle2, XCircle, AlertTriangle, Ban, Clock } from 'lucide-react';

export default function History() {
    const [executions, setExecutions] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetch('http://localhost:8080/api/executions?size=50')
            .then((res) => res.json())
            .then((data) => {
                // Dependiendo de cómo el backend envíe los datos (si es Page o List)
                setExecutions(data.content || data);
                setLoading(false);
            })
            .catch((err) => {
                console.error("Error al cargar historial:", err);
                setLoading(false);
            });
    }, []);

    const handleExport = (format) => {
        // Abre el endpoint de descarga directamente en una nueva pestaña
        window.open(`http://localhost:8080/api/executions/export/${format}`, '_blank');
    };

    const getStatusBadge = (status) => {
        switch (status) {
            case 'EXITOSO':
                return <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-green-50 text-green-700 border border-green-100"><CheckCircle2 size={14}/> Exitoso</span>;
            case 'ERROR':
                return <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-red-50 text-red-700 border border-red-100"><XCircle size={14}/> Error</span>;
            case 'ADVERTENCIA':
                return <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-orange-50 text-orange-700 border border-orange-100"><AlertTriangle size={14}/> Advertencia</span>;
            case 'OMITIDO':
                return <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-700 border border-gray-200"><Ban size={14}/> Omitido</span>;
            case 'EN_EJECUCION':
                return <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-blue-50 text-blue-700 border border-blue-100"><Clock size={14} className="animate-pulse"/> En progreso</span>;
            default:
                return <span>{status}</span>;
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return '---';
        const date = new Date(dateString);
        return date.toLocaleString('es-CR', {
            day: '2-digit', month: '2-digit', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    };

    return (
        <div className="max-w-6xl mx-auto space-y-8 animate-in fade-in duration-500">
            <div className="flex justify-between items-end">
                <div>
                    <h2 className="text-3xl font-semibold tracking-tight text-gray-900">Historial de Ejecuciones</h2>
                    <p className="text-gray-500 mt-2">Evidencia y auditoría de todos los respaldos realizados.</p>
                </div>
                <div className="flex gap-3">
                    <button onClick={() => handleExport('csv')} className="flex items-center gap-2 bg-white border border-gray-200 hover:bg-gray-50 text-gray-700 px-4 py-2.5 rounded-xl font-medium transition-colors shadow-sm">
                        <FileText size={18} className="text-gray-500" />
                        CSV
                    </button>
                    <button onClick={() => handleExport('pdf')} className="flex items-center gap-2 bg-white border border-gray-200 hover:bg-gray-50 text-gray-700 px-4 py-2.5 rounded-xl font-medium transition-colors shadow-sm">
                        <Download size={18} className="text-red-500" />
                        PDF
                    </button>
                </div>
            </div>

            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                {loading ? (
                    <div className="flex justify-center py-12">
                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                    </div>
                ) : executions.length === 0 ? (
                    <div className="text-center py-12 text-gray-500">No hay ejecuciones registradas en el historial.</div>
                ) : (
                    <table className="w-full text-left border-collapse">
                        <thead>
                        <tr className="bg-gray-50/50 border-b border-gray-100">
                            <th className="px-6 py-4 text-sm font-medium text-gray-500">Fecha de Inicio</th>
                            <th className="px-6 py-4 text-sm font-medium text-gray-500">Estrategia</th>
                            <th className="px-6 py-4 text-sm font-medium text-gray-500">Tipo</th>
                            <th className="px-6 py-4 text-sm font-medium text-gray-500">Origen</th>
                            <th className="px-6 py-4 text-sm font-medium text-gray-500">Duración</th>
                            <th className="px-6 py-4 text-sm font-medium text-gray-500">Estado</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                        {executions.map((exec) => (
                            <tr key={exec.id} className="hover:bg-gray-50/50 transition-colors">
                                <td className="px-6 py-4 font-medium text-gray-900">{formatDate(exec.startTime)}</td>
                                <td className="px-6 py-4">
                                    <div className="font-medium text-gray-900">{exec.strategyName}</div>
                                    <div className="text-sm text-gray-500">{exec.databaseName}</div>
                                </td>
                                <td className="px-6 py-4 text-gray-700">{exec.backupType}</td>
                                <td className="px-6 py-4 text-gray-700">{exec.origin}</td>
                                <td className="px-6 py-4 text-gray-700">{exec.durationSeconds ? `${exec.durationSeconds} seg` : '---'}</td>
                                <td className="px-6 py-4">{getStatusBadge(exec.status)}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                )}
            </div>
        </div>
    );
}