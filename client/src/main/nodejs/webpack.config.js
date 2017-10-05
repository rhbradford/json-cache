// Author: Richard Bradford

const path = require('path')
const HtmlWebpackPlugin = require('html-webpack-plugin')

module.exports = {
    context: path.resolve(__dirname, 'app'),
    entry:   './index.tsx',
    output:  {
        publicPath: '/assets/',
        filename:   'bundle.js',
        path:       path.resolve(__dirname, '..', '..', '..', 'build', 'resources', 'main', 'static', 'assets')
    },
    devtool: 'source-map',
    plugins: [
        new HtmlWebpackPlugin({
            template: './index.html',
            inject:   'body',
            filename: 'index.html'
        })
    ],
    resolve: {
        extensions: ['.js', '.ts', '.tsx']
    },
    module:  {
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
                            // Re-direct Semantic UI CSS theme to find assets from installed semantic-ui-css package
                            alias: {
                                './themes/default': 'semantic-ui-css/themes/default'
                            }
                        }
                    }
                ]
            },
            {
                test: /\.woff2?$|\.ttf$|\.eot$|\.svg$|\.png$/,
                use:  [
                    {
                        loader: 'url-loader'
                    }
                ]
            },
            {
                test:    /\.ts$|.tsx$/,
                exclude: path.resolve(__dirname, 'node_modules'),
                use:     [
                    {
                        loader: 'ts-loader'
                    }
                ]
            }
        ]
    }
}