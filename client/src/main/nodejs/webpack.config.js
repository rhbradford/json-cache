const path = require('path')
const webpack = require('webpack')
const HtmlWebpackPlugin = require('html-webpack-plugin')

module.exports = {
    context: path.resolve(__dirname, 'app'),
    entry: './index.js',
    output: {
        publicPath: '/assets/',
        filename: 'bundle.js',
        path: path.resolve(__dirname, '..', '..', '..', 'build', 'classes', 'static', 'assets')
    },
    plugins: [
        new HtmlWebpackPlugin({
            template: './index.html',
            inject: 'body',
            filename: 'index.html'
        })
    ],
    module: {
        rules: [
            {
                test: /\.css$/,
                use:  [
                    {
                        loader: 'style-loader'
                    },
                    {
                        loader: 'css-loader',
                        options: {
                            alias: {
                                '../fonts/bootstrap': 'bootstrap/dist/fonts',
                                '../fonts': 'bootstrap/dist/fonts'
                            }
                        }
                    }
                ]
            },
            {
                test: /\.woff2?$|\.ttf$|\.eot$|\.svg$/,
                use: [
                    {
                        loader: 'file-loader'
                    }
                ]
            },
            {
                test: /\.js$/,
                exclude: path.resolve(__dirname, 'node_modules'),
                use: [
                    {
                        loader: 'babel-loader',
                        options: {
                            presets: ['env','es2016','react'],
                            plugins: [
                                require('babel-plugin-transform-object-rest-spread'),
                                require('babel-plugin-transform-runtime')
                            ]
                        }
                    }
                ]
            }
        ]
    }
}